package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FontPreset

val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val PlusJakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val PlayfairFontFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val PacificoFontFamily = FontFamily(Font(R.font.pacifico, FontWeight.Normal))
val RobotoMonoFontFamily = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal))

fun fontFamilyForPreset(preset: FontPreset): FontFamily = when (preset) {
    FontPreset.INTER -> InterFontFamily
    FontPreset.PLUS_JAKARTA_SANS -> PlusJakartaFontFamily
    FontPreset.PLAYFAIR_DISPLAY -> PlayfairFontFamily
    FontPreset.PACIFICO -> PacificoFontFamily
    FontPreset.ROBOTO_MONO -> RobotoMonoFontFamily
}

fun buildInstaPlusTypography(preset: FontPreset): Typography {
    val activeFamily = fontFamilyForPreset(preset)
    val brandFamily = if (preset == FontPreset.INTER) PacificoFontFamily else activeFamily
    return Typography(
        displaySmall = TextStyle(
            fontFamily = brandFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}
