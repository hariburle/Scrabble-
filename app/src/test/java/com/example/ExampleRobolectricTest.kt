package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.dictionary.WordDictionary
import com.example.game.engine.ScrabbleEngine
import com.example.game.model.PlacedTile
import com.example.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun read_app_name_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Scrabble", appName)
    }

    @Test
    fun test_dictionary_and_engine() {
        WordDictionary.init()
        assertTrue(WordDictionary.isValidWord("HELLO"))
        assertTrue(WordDictionary.isValidWord("QI"))
        assertTrue(WordDictionary.isValidWord("ZEN"))
        assertTrue(WordDictionary.isValidWord("SEX"))
        assertTrue(WordDictionary.isValidWord("SEXY"))
        assertTrue(WordDictionary.isValidWord("SEXES"))
        assertTrue(WordDictionary.isValidWord("SEXED"))
        assertTrue(WordDictionary.isValidWord("GENT"))
        assertTrue(WordDictionary.isValidWord("GENTS"))
        assertTrue(WordDictionary.isValidWord("GENTLE"))
        assertTrue(WordDictionary.isValidWord("SCRABBLE"))
        assertTrue(WordDictionary.hasPrefix("GEN"))
        assertTrue(WordDictionary.hasPrefix("SEX"))
        assertTrue(WordDictionary.hasPrefix("SCR"))

        val board = ScrabbleEngine.createEmptyBoard()
        val placements = listOf(
            PlacedTile(7, 7, Tile(letter = 'H')),
            PlacedTile(7, 8, Tile(letter = 'I'))
        )
        val scoreResult = ScrabbleEngine.evaluateMove(board, placements, isFirstMove = true)
        assertTrue(scoreResult.isValid)
        assertTrue(scoreResult.totalScore > 0)
    }
}
