package com.stunmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stunmap.classifier.IpClassification
import com.stunmap.ui.theme.CandidateGreen
import com.stunmap.ui.theme.JetBrainsMonoFamily
import com.stunmap.ui.theme.KnownStunBlue
import com.stunmap.ui.theme.MetaRed
import com.stunmap.ui.theme.PrivateGray
import com.stunmap.ui.theme.SelfAmber
import com.stunmap.ui.theme.UnknownGray

@Composable
fun ClassificationBadge(classification: IpClassification, modifier: Modifier = Modifier) {
    val (label, color) = when (classification) {
        IpClassification.CANDIDATE -> "CANDIDATE" to CandidateGreen
        IpClassification.META_INFRA -> "META_INFRA" to MetaRed
        IpClassification.SELF -> "SELF" to SelfAmber
        IpClassification.KNOWN_STUN -> "STUN_SERVER" to KnownStunBlue
        IpClassification.PRIVATE -> "PRIVATE" to PrivateGray
        IpClassification.UNKNOWN -> "UNKNOWN" to UnknownGray
    }

    Text(
        text = label,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = color,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    )
}
