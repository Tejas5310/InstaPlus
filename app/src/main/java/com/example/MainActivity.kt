package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.TranslationRepository
import com.example.data.local.DefaultGhostModeRepository
import com.example.data.local.InstaPlusAccountAuthRepository
import com.example.data.local.InstaPlusAuthSession
import com.example.data.local.InstaPlusDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SocialPlatformRepository
import com.example.data.local.VaultAndAccountRepository
import com.example.data.media.MediaStoreDownloader
import com.example.data.model.InstaPlusSection
import com.example.data.model.InstaPlusSettings
import com.example.data.remote.FirestoreCloudRepository
import com.example.network.GhostModeOkHttpInterceptor
import com.example.security.configureAppCheck
import com.example.system.AppSystemBridge
import com.example.ui.components.AccountSwitcherSheet
import com.example.ui.components.ChatLockOverlay
import com.example.ui.components.InstaPlusBottomBar
import com.example.ui.components.InstaPlusNavigationRail
import com.example.ui.components.InstaPlusTopBar
import com.example.ui.components.LockedChatWrapper
import com.example.ui.components.MediaDownloadBottomSheet
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.TranslationBottomSheet
import com.example.ui.navigation.FeedRoute
import com.example.ui.navigation.GhostModeRoute
import com.example.ui.navigation.ModHubRoute
import com.example.ui.navigation.VaultRoute
import com.example.ui.screens.AuthScreenMode
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.GhostModeScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ModHubScreen
import com.example.ui.screens.PostComposerSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.InstaPlusTheme
import com.example.ui.viewmodel.InstaPlusViewModel
import com.example.ui.viewmodel.LockChallenge
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before Compose UI or ViewModels initialize.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            InstaPlusAuthGate()
        }
    }
}

@Composable
fun InstaPlusAuthGate() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val authRepo = remember(appContext) { InstaPlusAccountAuthRepository(appContext) }
    val prefsRepo = remember(appContext) { PreferencesRepository(appContext) }
    val scope = rememberCoroutineScope()

    val settings by prefsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = InstaPlusSettings())
    val activeSession by authRepo.activeSessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val allAccounts by authRepo.allAccountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var addAccountMode by remember { mutableStateOf<AuthScreenMode?>(null) }

    val session = activeSession

    if (session == null || addAccountMode != null) {
        InstaPlusTheme(
            isDarkMode = settings.isDarkMode,
            themePreset = settings.themePreset,
            accentPreset = settings.accentPreset,
            fontPreset = settings.fontPreset
        ) {
            SignInScreen(
                authRepo = authRepo,
                isDarkMode = settings.isDarkMode,
                onToggleDarkMode = { enabled ->
                    scope.launch { prefsRepo.setDarkMode(enabled) }
                },
                savedAccounts = allAccounts,
                initialMode = addAccountMode ?: AuthScreenMode.LOG_IN,
                onQuickSwitchAccount = { uid ->
                    scope.launch {
                        authRepo.switchActiveAccount(uid)
                        addAccountMode = null
                    }
                },
                onCancelToApp = if (session != null) {
                    { addAccountMode = null }
                } else null
            )
        }
    } else {
        AuthenticatedInstaPlusHost(
            session = session,
            allAccounts = allAccounts,
            authRepo = authRepo,
            onRequestAddAccount = { createNew ->
                addAccountMode = if (createNew) AuthScreenMode.CREATE_ACCOUNT else AuthScreenMode.LOG_IN
            }
        )
    }
}

@Composable
private fun AuthenticatedInstaPlusHost(
    session: InstaPlusAuthSession,
    allAccounts: List<InstaPlusAuthSession>,
    authRepo: InstaPlusAccountAuthRepository,
    onRequestAddAccount: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val firebaseUser = remember { Firebase.auth.currentUser }
    val effectiveUid = firebaseUser?.uid ?: session.uid

    key(effectiveUid) {
        val viewModel: InstaPlusViewModel = viewModel(
            key = effectiveUid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val appContext = app.applicationContext
                    val databaseId = appContext.getString(R.string.firestore_database_id)
                    val firestore = FirebaseFirestore.getInstance(databaseId)
                    val localDb = InstaPlusDatabase.getInstance(appContext)
                    val prefsRepo = PreferencesRepository(appContext)
                    val ghostModeRepo = DefaultGhostModeRepository(prefsRepo)
                    val ghostOkHttpInterceptor = GhostModeOkHttpInterceptor(ghostModeRepo)
                    val socialRepo = SocialPlatformRepository(
                        postDao = localDb.socialPostDao(),
                        commentDao = localDb.postCommentDao(),
                        storyDao = localDb.storyDao(),
                        directChatDao = localDb.directChatDao(),
                        ghostModeRepository = ghostModeRepo
                    )
                    InstaPlusViewModel(
                        currentUserId = effectiveUid,
                        currentUserName = session.username,
                        currentFullName = session.fullName,
                        currentMobileNumber = session.mobileNumber,
                        currentUserEmail = session.email,
                        hasFirebaseCloudAuth = firebaseUser != null,
                        prefsRepo = prefsRepo,
                        ghostModeRepo = ghostModeRepo,
                        vaultAccountRepo = VaultAndAccountRepository(
                            mediaDao = localDb.downloadedMediaDao(),
                            accountDao = localDb.accountSessionDao(),
                            lockedChatDao = localDb.lockedChatDao()
                        ),
                        socialRepo = socialRepo,
                        cloudRepo = FirestoreCloudRepository(firestore),
                        downloader = MediaStoreDownloader(appContext, ghostOkHttpInterceptor),
                        translator = TranslationRepository(),
                        systemBridge = AppSystemBridge(appContext)
                    )
                }
            }
        )
        InstaPlusApp(
            viewModel = viewModel,
            signedInSession = session,
            allInstaPlusAccounts = allAccounts,
            onSwitchInstaPlusAccount = { target ->
                scope.launch { authRepo.switchActiveAccount(target.uid) }
            },
            onAddInstaPlusAccount = onRequestAddAccount,
            onRemoveInstaPlusAccount = { uid ->
                scope.launch { authRepo.removeAccount(uid) }
            },
            onSignOut = {
                scope.launch { authRepo.signOut() }
            }
        )
    }
}

@Composable
fun InstaPlusApp(
    viewModel: InstaPlusViewModel,
    signedInSession: InstaPlusAuthSession,
    allInstaPlusAccounts: List<InstaPlusAuthSession>,
    onSwitchInstaPlusAccount: (InstaPlusAuthSession) -> Unit,
    onAddInstaPlusAccount: (Boolean) -> Unit,
    onRemoveInstaPlusAccount: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ghostPreferences by viewModel.ghostPreferences.collectAsStateWithLifecycle()
    val ghostInterceptedRequests by viewModel.ghostInterceptedRequests.collectAsStateWithLifecycle()
    val ghostBlockedCounts by viewModel.ghostBlockedCounts.collectAsStateWithLifecycle()
    val downloadedMedia by viewModel.downloadedMedia.collectAsStateWithLifecycle()
    val lockedChats by viewModel.lockedChats.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    val posts by viewModel.socialPosts.collectAsStateWithLifecycle()
    val comments by viewModel.postComments.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val directThreads by viewModel.directThreads.collectAsStateWithLifecycle()
    val directMessages by viewModel.directMessages.collectAsStateWithLifecycle()
    val userBio by viewModel.userBio.collectAsStateWithLifecycle()

    val activeSection by viewModel.activeSection.collectAsStateWithLifecycle()
    val activeThreadKey by viewModel.activeThreadKey.collectAsStateWithLifecycle()
    val focusSecondsElapsed by viewModel.focusSecondsElapsed.collectAsStateWithLifecycle()
    val showFocusBreakOverlay by viewModel.showFocusBreakOverlay.collectAsStateWithLifecycle()

    val showComposerSheet by viewModel.showComposerSheet.collectAsStateWithLifecycle()
    val aiCaptionState by viewModel.aiCaptionState.collectAsStateWithLifecycle()

    val detectedMedia by viewModel.detectedMedia.collectAsStateWithLifecycle()
    val showDownloadSheet by viewModel.showDownloadSheet.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val showTranslationSheet by viewModel.showTranslationSheet.collectAsStateWithLifecycle()
    val translationSource by viewModel.translationSourceText.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()

    val showAccountSwitcher by viewModel.showAccountSwitcher.collectAsStateWithLifecycle()
    val pinSetupMode by viewModel.pinSetupMode.collectAsStateWithLifecycle()
    val activeLockChallenge by viewModel.activeLockChallenge.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFeedRoute = currentRoute == null || currentRoute.contains("FeedRoute")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    InstaPlusTheme(
        isDarkMode = settings.isDarkMode,
        themePreset = settings.themePreset,
        accentPreset = settings.accentPreset,
        fontPreset = settings.fontPreset
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useSideRail = maxWidth >= 700.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (isFeedRoute) {
                            InstaPlusTopBar(
                                activeUsername = signedInSession.username,
                                settings = settings,
                                vaultCount = downloadedMedia.size,
                                focusSecondsElapsed = focusSecondsElapsed,
                                onAccountSwitchClick = { viewModel.openAccountSwitcher() },
                                onCreatePostClick = { viewModel.openComposerSheet() },
                                onToggleThemeClick = viewModel::toggleDarkMode,
                                onGhostClick = { navController.navigate(GhostModeRoute) },
                                onVaultClick = { navController.navigate(VaultRoute) },
                                onModHubClick = { navController.navigate(ModHubRoute) }
                            )
                        }
                    },
                    bottomBar = {
                        if (isFeedRoute && !useSideRail) {
                            InstaPlusBottomBar(
                                activeSection = activeSection,
                                hideReelsTab = settings.hideReelsTab,
                                isDmLocked = settings.isDmTabLocked || lockedChats.isNotEmpty(),
                                onSelectSection = viewModel::selectSection
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isFeedRoute && useSideRail) {
                            InstaPlusNavigationRail(
                                activeSection = activeSection,
                                hideReelsTab = settings.hideReelsTab,
                                isDmLocked = settings.isDmTabLocked || lockedChats.isNotEmpty(),
                                onSelectSection = viewModel::selectSection
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = FeedRoute,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable<FeedRoute> {
                                    if (activeSection != InstaPlusSection.HOME) {
                                        BackHandler { viewModel.selectSection(InstaPlusSection.HOME) }
                                    }

                                    when (activeSection) {
                                        InstaPlusSection.HOME -> FeedScreen(
                                            settings = settings,
                                            currentUsername = signedInSession.username,
                                            posts = posts,
                                            comments = comments,
                                            stories = stories,
                                            onToggleLike = viewModel::toggleLike,
                                            onToggleBookmark = viewModel::toggleBookmark,
                                            onToggleFollow = viewModel::toggleFollowAuthor,
                                            onVotePoll = viewModel::votePoll,
                                            onAddComment = viewModel::addComment,
                                            onStoryViewed = viewModel::markStoryViewed,
                                            onSavePostToVault = viewModel::savePostToVault,
                                            onSaveStoryToVault = viewModel::saveStoryToVault,
                                            onCopyText = viewModel::copyTextWithToast,
                                            onTranslateText = viewModel::openTranslationWithText,
                                            onSharePost = viewModel::sharePostText,
                                            onOpenCreateComposer = viewModel::openComposerSheet,
                                            onOpenDownloadSheet = viewModel::openDownloadSheetForFeed,
                                            onToggleGhostClick = viewModel::toggleGhostMaster
                                        )

                                        InstaPlusSection.SEARCH -> ExploreScreen(
                                            posts = posts,
                                            onToggleLike = viewModel::toggleLike,
                                            onSaveToVault = viewModel::savePostToVault,
                                            onCopyCaption = { post ->
                                                viewModel.copyTextWithToast("Caption", post.caption)
                                            }
                                        )

                                        InstaPlusSection.REELS -> ReelsScreen(
                                            posts = posts,
                                            comments = comments,
                                            onToggleLike = viewModel::toggleLike,
                                            onToggleBookmark = viewModel::toggleBookmark,
                                            onAddComment = viewModel::addComment,
                                            onCopyText = viewModel::copyTextWithToast,
                                            onTranslateText = viewModel::openTranslationWithText,
                                            onSaveToVault = viewModel::savePostToVault,
                                            onSharePost = viewModel::sharePostText,
                                            onCreateReelClick = viewModel::openComposerSheet
                                        )

                                        InstaPlusSection.MESSAGES -> {
                                            val chatChallenge = activeLockChallenge?.takeIf { it != LockChallenge.AppLaunch }
                                            val isProtected = remember(activeThreadKey, settings.isDmTabLocked, lockedChats) {
                                                viewModel.isCurrentConversationProtected()
                                            }
                                            LockedChatWrapper(
                                                isInDirectMessages = true,
                                                activeThreadLabel = activeThreadKey,
                                                isCurrentChatProtected = isProtected,
                                                activeLockChallenge = chatChallenge,
                                                hasPinConfigured = settings.hasPinConfigured,
                                                isBiometricEnabled = settings.isBiometricEnabled,
                                                pinError = pinError,
                                                onRequestLockCurrentChat = {
                                                    val key = activeThreadKey.orEmpty()
                                                    viewModel.lockActiveConversationFromWrapper(key, key.ifBlank { "Messages" })
                                                },
                                                onRelockNow = viewModel::relockCurrentChatNow,
                                                onAutoRelockOnBackground = viewModel::relockAllOnBackground,
                                                onPinSubmitted = viewModel::submitUnlockPin,
                                                onBiometricSuccess = viewModel::onBiometricUnlockSucceeded,
                                                onCancelToHome = viewModel::dismissLockBackToHome
                                            ) {
                                                MessagesScreen(
                                                    settings = settings,
                                                    threads = directThreads,
                                                    messages = directMessages,
                                                    lockedChats = lockedChats,
                                                    activeThreadKey = activeThreadKey,
                                                    onOpenThread = viewModel::openChatThread,
                                                    onCloseThread = viewModel::closeChatThread,
                                                    onToggleThreadLock = viewModel::toggleThreadLock,
                                                    onSendMessage = viewModel::sendDirectMessage,
                                                    onTypingChanged = viewModel::notifyTypingInThread,
                                                    onCreateThread = viewModel::startNewChatThread,
                                                    onCopyText = viewModel::copyTextWithToast,
                                                    onTranslateText = viewModel::openTranslationWithText
                                                )
                                            }
                                        }

                                        InstaPlusSection.PROFILE -> ProfileScreen(
                                            session = signedInSession,
                                            userBio = userBio,
                                            allPosts = posts,
                                            vaultCount = downloadedMedia.size,
                                            blockedGhostCount = ghostBlockedCounts.values.sum(),
                                            onUpdateBio = viewModel::updateUserBio,
                                            onCopyText = viewModel::copyTextWithToast,
                                            onCreatePostClick = viewModel::openComposerSheet,
                                            onOpenVault = { navController.navigate(VaultRoute) },
                                            onOpenModHub = { navController.navigate(ModHubRoute) },
                                            onDeletePost = viewModel::deletePost
                                        )
                                    }
                                }

                                composable<VaultRoute> {
                                    VaultScreen(
                                        items = downloadedMedia,
                                        onBackClick = { navController.popBackStack() },
                                        onDeleteItem = viewModel::deleteDownloadedMedia,
                                        onOpenManualDownloader = viewModel::openManualDownloadSheet
                                    )
                                }

                                composable<ModHubRoute> {
                                    ModHubScreen(
                                        settings = settings,
                                        lockedChats = lockedChats,
                                        accountCount = allInstaPlusAccounts.size,
                                        vaultCount = downloadedMedia.size,
                                        blockedGhostCount = ghostBlockedCounts.values.sum(),
                                        cloudSyncState = cloudSyncState,
                                        onSyncNow = viewModel::syncAllLocalToCloud,
                                        onSignOut = onSignOut,
                                        onBackClick = { navController.popBackStack() },
                                        onOpenGhostMode = { navController.navigate(GhostModeRoute) },
                                        onBooleanSettingChange = viewModel::updateBooleanSetting,
                                        onDarkModeToggle = viewModel::setDarkMode,
                                        onThemeSelect = viewModel::setThemePreset,
                                        onAccentSelect = viewModel::setAccentPreset,
                                        onFontSelect = viewModel::setFontPreset,
                                        onChatBgSelect = viewModel::setChatBackgroundPreset,
                                        onCustomChatBgPicked = viewModel::setCustomChatBackground,
                                        onLauncherIconSelect = viewModel::setLauncherIcon,
                                        onOpenPinSetup = viewModel::openPinSetup,
                                        onAddLockedChat = viewModel::addLockedChat,
                                        onRemoveLockedChat = viewModel::removeLockedChat,
                                        onTargetLanguageSelect = viewModel::setTargetLanguage,
                                        onFocusLimitSelect = viewModel::setFocusLimitMinutes,
                                        onOpenTranslator = { viewModel.openTranslationWithText("") },
                                        onOpenVault = { navController.navigate(VaultRoute) },
                                        onOpenAccountSwitcher = viewModel::openAccountSwitcher
                                    )
                                }

                                composable<GhostModeRoute> {
                                    GhostModeScreen(
                                        preferences = ghostPreferences,
                                        interceptedRequests = ghostInterceptedRequests,
                                        blockedCounts = ghostBlockedCounts,
                                        onBackClick = { navController.popBackStack() },
                                        onMasterToggle = viewModel::setGhostMaster,
                                        onStorySeenToggle = viewModel::setGhostStorySeenHidden,
                                        onDmSeenToggle = viewModel::setGhostDmSeenHidden,
                                        onTypingToggle = viewModel::setGhostTypingHidden,
                                        onPresenceToggle = viewModel::setGhostPresenceHidden,
                                        onLoggingToggle = viewModel::setGhostLoggingEnabled,
                                        onClearLog = viewModel::clearGhostInterceptLog
                                    )
                                }
                            }
                        }
                    }
                }

                // Top-level App Launch Lock Screen Overlay
                if (activeLockChallenge == LockChallenge.AppLaunch) {
                    ChatLockOverlay(
                        challenge = LockChallenge.AppLaunch,
                        hasPinConfigured = settings.hasPinConfigured,
                        isBiometricEnabled = settings.isBiometricEnabled,
                        errorMessage = pinError,
                        onPinSubmitted = viewModel::submitUnlockPin,
                        onBiometricSuccess = viewModel::onBiometricUnlockSucceeded,
                        onCancelToHome = null
                    )
                }
            }

            if (showComposerSheet) {
                PostComposerSheet(
                    aiCaptionState = aiCaptionState,
                    onGenerateAiCaption = viewModel::generateAiCaption,
                    onPublish = viewModel::publishStandalonePost,
                    onDismiss = viewModel::dismissComposerSheet
                )
            }

            if (showDownloadSheet) {
                MediaDownloadBottomSheet(
                    detectedMedia = detectedMedia,
                    downloadState = downloadState,
                    onDownloadItem = viewModel::downloadMediaItem,
                    onDownloadManualUrl = viewModel::downloadManualUrl,
                    onDismiss = viewModel::dismissDownloadSheet
                )
            }

            if (showTranslationSheet) {
                TranslationBottomSheet(
                    sourceText = translationSource,
                    targetLanguage = settings.targetLanguage,
                    translationState = translationState,
                    onSourceTextChange = viewModel::updateTranslationSourceText,
                    onTargetLanguageSelected = viewModel::setTargetLanguage,
                    onTranslateClick = viewModel::translateText,
                    onDismiss = viewModel::dismissTranslationSheet
                )
            }

            if (showAccountSwitcher) {
                AccountSwitcherSheet(
                    instaPlusAccounts = allInstaPlusAccounts,
                    activeInstaPlusUid = signedInSession.uid,
                    onSwitchInstaPlusAccount = { target ->
                        viewModel.dismissAccountSwitcher()
                        onSwitchInstaPlusAccount(target)
                    },
                    onAddInstaPlusAccount = { createNew ->
                        viewModel.dismissAccountSwitcher()
                        onAddInstaPlusAccount(createNew)
                    },
                    onRemoveInstaPlusAccount = onRemoveInstaPlusAccount,
                    onDismiss = viewModel::dismissAccountSwitcher
                )
            }

            pinSetupMode?.let { mode ->
                PinSetupDialog(
                    mode = mode,
                    onSavePin = viewModel::saveNewPin,
                    onDismiss = viewModel::dismissPinSetup
                )
            }

            if (showFocusBreakOverlay) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissFocusBreakReset,
                    title = { Text("Mindful Scroll Guard") },
                    text = {
                        Column {
                            Text(
                                "You've reached your ${settings.focusLimitMinutes}-minute session goal. " +
                                    "Take a mindful breather or reset the timer to continue."
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = viewModel::dismissFocusBreakReset) {
                            Text("Reset Timer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.updateBooleanSetting("focus_guard_enabled", false)
                            viewModel.dismissFocusBreakReset()
                        }) {
                            Text("Pause Guard")
                        }
                    }
                )
            }
        }
    }
}
