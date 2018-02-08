package johnwilde.androidchessclock

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class AppModule{
    @Provides
    fun providesContext(application: ChessApplication): Context {
        return application.applicationContext
    }
}