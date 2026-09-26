package com.streamvault

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streamvault.ui.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTests {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allFiveDestinationsAndPlaylistCreationAreReachable() {
        compose.onNodeWithText("Un universo de música.").assertIsDisplayed()
        compose.onNodeWithText("Biblioteca", useUnmergedTree = true).performClick()
        compose.onNodeWithText("TU UNIVERSO SONORO").assertIsDisplayed()
        compose.onNodeWithText("Buscar", useUnmergedTree = true).performClick()
        // The search screen keeps the phone and the internet in separate tabs.
        compose.onNodeWithText("BUSCAR").assertIsDisplayed()
        compose.onNodeWithText("Teléfono").assertIsDisplayed()
        compose.onNodeWithText("Online").assertIsDisplayed()
        compose.onNodeWithText("Todos").assertIsDisplayed()
        compose.onNodeWithText("Canción, artista o álbum…").performTextInput("no-existe-12345")
        compose.onNodeWithText("Biblioteca", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Canciones, artistas, carpetas…").performTextInput("no-existe-12345")
        compose.onNodeWithText("Ajustes", useUnmergedTree = true).performClick()
        compose.onNodeWithText("A TU MANERA").assertIsDisplayed()
        compose.onNodeWithText("Crossfade").performClick()
        compose.onNode(hasText("10 segundos") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("10 segundos · curva de potencia constante").assertIsDisplayed()
        compose.onNodeWithText("Playlists", useUnmergedTree = true).performClick()
        compose.onNodeWithText(" Crear playlist").performClick()
        compose.onNodeWithText("Nombre").performTextInput("Lista de prueba de navegación")
        compose.onNodeWithText("Descripción").performTextInput("Creada en prueba instrumentada")
        compose.onNodeWithText("Guardar").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Lista de prueba de navegación").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasTestTag("playlist-row")).fetchSemanticsNodes().let { rows ->
            assertTrue("Debe existir la fila de la playlist (filas=${rows.size})", rows.isNotEmpty())
        }
        compose.onAllNodes(hasTestTag("playlist-row"))[0].performScrollTo().performClick()
        // The detail view replaces the list: wait for its own actions before asserting on it.
        val opened = runCatching {
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Eliminar playlist").fetchSemanticsNodes().isNotEmpty() }
        }.isSuccess
        if (!opened) {
            // Encode what is on screen into the failure message: it is the only output this job gives.
            val back = compose.onAllNodesWithText(" Playlists").fetchSemanticsNodes().size
            val play = compose.onAllNodesWithText("Escuchar").fetchSemanticsNodes().size
            val rows = compose.onAllNodesWithText("Lista de prueba de navegación").fetchSemanticsNodes().size
            val chips = compose.onAllNodesWithText("Locales").fetchSemanticsNodes().size
            assertTrue("No se abrió la playlist (atras=$back escuchar=$play filas=$rows chips=$chips)", false)
        }
        compose.onNodeWithContentDescription("Eliminar playlist").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Eliminar playlist").performClick()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("Tus playlists").assertIsDisplayed()
        compose.onNodeWithText("Inicio", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Un universo de música.").assertIsDisplayed()
    }
}
