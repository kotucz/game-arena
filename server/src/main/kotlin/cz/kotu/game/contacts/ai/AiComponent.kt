package cz.kotu.game.contacts.ai

import cz.kotu.gamearena.AiPlayerConfig
import cz.kotu.gamearena.ServerScope
import me.tatarka.inject.annotations.Provides

@ServerScope
interface AiComponent {
    @Provides
    @ServerScope
    fun provideContactsAiPlayer(aiPlayerConfig: AiPlayerConfig): ContactsAiPlayer =
        ContactsAiPlayer(aiPlayerConfig = aiPlayerConfig)

    @Provides
    @ServerScope
    fun provideContactsAiStart(contactsAiPlayer: ContactsAiPlayer): ContactsAiStart = contactsAiPlayer
}
