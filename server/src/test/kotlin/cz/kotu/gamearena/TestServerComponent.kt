package cz.kotu.gamearena

import me.tatarka.inject.annotations.Component

@Component
abstract class TestServerComponent(@Component val fakes: TestFakes = TestFakes()) : ServerBindings {
    abstract override val database: AppDatabase
    abstract override val gamesManager: GamesManager

}
