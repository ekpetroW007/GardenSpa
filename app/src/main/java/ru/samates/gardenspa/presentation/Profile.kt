package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import ru.samates.gardenspa.viewmodel.UserViewModel

/** Kept for restored destinations from versions with a separate Today screen. */
@Composable
fun Profile(
    navController: NavController,
    onScreenSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel
) {
    Calendar(PaddingValues(), navController, userViewModel = userViewModel,
        onScreenSelected = onScreenSelected, modifier = modifier)
}
