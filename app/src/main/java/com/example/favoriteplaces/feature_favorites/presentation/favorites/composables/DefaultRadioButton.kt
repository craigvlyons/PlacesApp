package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun DefaultRadioButton(
    text:String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ){
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary,
                unselectedColor = MaterialTheme.colors.onBackground
            ),
        )
        Text(text = text, style = MaterialTheme.typography.body2)
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultRadioButtonPreview() {
    DefaultRadioButton(
        text = "Example Radio Button",
        selected = true,
        onSelect = {}
    )
}