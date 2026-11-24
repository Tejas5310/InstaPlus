const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const firebaseCompatRoot = path.dirname(
  require.resolve("@firebase/rules-unit-testing/package.json")
);
const firebase = require(path.join(firebaseCompatRoot, "node_modules/firebase/compat/app"));
require(path.join(firebaseCompatRoot, "node_modules/firebase/compat/firestore"));
const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid) {
  return {
    userId: uid,
    displayName: "Alice Tester",
    email: "alice@example.com",
    highResPriority: true,
    autoDetectMedia: true,
    showFloatingActionDock: true,
    hideOnlineStatus: true,
    anonymousStoryViewing: true,
    disableReadReceipts: true,
    hideTypingIndicator: true,
    blockFeedAds: true,
    blockStoryAds: true,
    blockReelsAds: true,
    hideSuggestedPosts: false,
    themePreset: "INSTAGRAM_LIGHT",
    fontPreset: "INTER",
    chatBackgroundPreset: "DEFAULT",
    hideReelsTab: false,
    launcherIconVariant: "CLASSIC",
    isDmTabLocked: false,
    isAppLockEnabled: false,
    isBiometricEnabled: true,
    targetLanguage: "English",
    autoOfferTranslate: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validVaultPayload(uid) {
  return {
    userId: uid,
    mediaType: "REEL",
    authorHandle: "creator_one",
    captionSnippet: "Sunset timelapse in Kyoto",
    sourceUrl: "https://cdn.example.com/video/123.mp4",
    fileName: "reel_creator_one_123.mp4",
    mimeType: "video/mp4",
    fileSizeBytes: 2048000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validLockedChatPayload(uid) {
  return {
    userId: uid,
    threadKey: "340282366841710300949128",
    displayName: "@best_friend",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validAccountProfilePayload(uid) {
  return {
    userId: uid,
    username: "instaplus_creator",
    displayName: "Creator Account",
    isActive: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated access must always fail
test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("vaultItems").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("lockedChats").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("accountProfiles").get()
  );
});

// 2. Owner can create, read, update, and delete UserCloudProfile
test("Owner can create, read, and update UserCloudProfile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      themePreset: "AMOLED_DARK",
      hideReelsTab: true,
      updatedAt: serverTimestamp(),
    })
  );
});

// 3. Cross-user isolation & identity spoofing tests
test("Authenticated user cannot read, create, or overwrite another user's profile or subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vaultItems")
      .doc("item_1")
      .set(validVaultPayload(ALICE_UID))
  );

  // Bob cannot read or modify Alice's profile or vault
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ themePreset: "SUNSET_ROSE", updatedAt: serverTimestamp() })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vaultItems").get()
  );
  // Bob cannot spoof userId inside his own document
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(validProfilePayload(ALICE_UID))
  );
});

// 4. Shadow field, enum poisoning, and immutable field tampering tests
test("Rules reject shadow fields, invalid enums, negative file sizes, and immutable createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost / shadow field on create
  await assertFails(
    profileRef.set({
      ...validProfilePayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid theme enum
  await assertFails(
    profileRef.set({
      ...validProfilePayload(ALICE_UID),
      themePreset: "HACKED_THEME",
    })
  );

  // Valid create
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));

  // Immutable userId or createdAt update fails
  await assertFails(
    profileRef.update({
      userId: BOB_UID,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    profileRef.update({
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Negative file size fails in vaultItems
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vaultItems")
      .doc("bad_item")
      .set({
        ...validVaultPayload(ALICE_UID),
        fileSizeBytes: -50,
      })
  );

  // Invalid document ID poisoning fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vaultItems")
      .doc("bad id with spaces!")
      .set(validVaultPayload(ALICE_UID))
  );
});

// 5. Subcollections CRUD (vaultItems, lockedChats, accountProfiles)
test("Owner can CRUD vaultItems, lockedChats, and accountProfiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  // Vault item
  const vaultRef = userDoc.collection("vaultItems").doc("vault_01");
  await assertSucceeds(vaultRef.set(validVaultPayload(ALICE_UID)));
  await assertSucceeds(userDoc.collection("vaultItems").get());
  await assertSucceeds(vaultRef.delete());

  // Locked chat
  const chatRef = userDoc.collection("lockedChats").doc("chat_01");
  await assertSucceeds(chatRef.set(validLockedChatPayload(ALICE_UID)));
  await assertSucceeds(userDoc.collection("lockedChats").get());
  await assertSucceeds(
    chatRef.update({ displayName: "@updated_friend", updatedAt: serverTimestamp() })
  );
  await assertSucceeds(chatRef.delete());

  // Account profile
  const accRef = userDoc.collection("accountProfiles").doc("acc_01");
  await assertSucceeds(accRef.set(validAccountProfilePayload(ALICE_UID)));
  await assertSucceeds(userDoc.collection("accountProfiles").get());
  await assertSucceeds(
    accRef.update({ isActive: false, updatedAt: serverTimestamp() })
  );
  await assertSucceeds(accRef.delete());
});
