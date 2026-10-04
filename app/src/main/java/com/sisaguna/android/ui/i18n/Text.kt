package com.sisaguna.android.ui.i18n

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.sisaguna.android.data.settings.AppLanguage
import androidx.compose.material3.Text as M3Text

/**
 * The app's copy is written in Indonesian. When the user picks English, every [Text] in the
 * app runs its string through [tr], which looks it up in [EnDictionary] (exact phrases) and
 * then [EnPatterns] (templated strings like "3 porsi"). Unknown strings fall through
 * unchanged, so a missing entry shows Indonesian instead of crashing or showing a key.
 *
 * Screens import `com.sisaguna.android.ui.i18n.Text` instead of Material's — same signature.
 */
object SgLocale {
    var language by mutableStateOf(AppLanguage.ID)
    val isEnglish: Boolean get() = language == AppLanguage.EN
}

fun tr(text: String): String {
    if (!SgLocale.isEnglish || text.isBlank()) return text
    EnDictionary.map[text]?.let { return it }
    val trimmed = text.trim()
    EnDictionary.map[trimmed]?.let { return text.replace(trimmed, it) }
    var out = text
    for ((regex, replacement) in EnPatterns.rules) out = regex.replace(out, replacement)
    return out
}

/**
 * Material's SnackbarHost draws its message with its own Text, so messages would skip [tr]
 * and stay Indonesian in English mode. This host renders through the translating [Text].
 */
@Composable
fun SgSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        // Ink surface inverts with the theme; the action takes a brand green that keeps
        // contrast on either.
        val action = if (SgColor.Ink.luminance() > 0.5f) SgColor.Brand700 else SgColor.Brand300
        Snackbar(
            // Sits just above whatever bottom bar the Scaffold placed it over.
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = SgColor.Ink,
            contentColor = SgColor.Page,
            action = data.visuals.actionLabel?.let { label ->
                {
                    TextButton(onClick = data::performAction) {
                        Text(label, color = action, style = SgTextStyle.Label)
                    }
                }
            },
        ) { Text(data.visuals.message, style = SgTextStyle.Label, color = SgColor.Page) }
    }
}

/** Pick between an Indonesian and English string inline, for copy built in code. */
fun l(id: String, en: String): String = if (SgLocale.isEnglish) en else id

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    M3Text(
        text = tr(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    // Styled spans would lose their ranges if the text were swapped, so only plain annotated
    // strings are translated.
    val shown = if (SgLocale.isEnglish && text.spanStyles.isEmpty() && text.paragraphStyles.isEmpty()) AnnotatedString(tr(text.text)) else text
    M3Text(
        text = shown,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}
