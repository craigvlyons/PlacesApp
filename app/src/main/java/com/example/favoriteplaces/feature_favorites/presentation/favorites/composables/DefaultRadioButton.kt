package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun DefaultRadioButton(
    text:String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier.padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ){
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary,
                unselectedColor = MaterialTheme.colors.onBackground
            ),
            modifier = Modifier.padding(start = 0.dp)
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