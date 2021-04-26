package ru.hadron.morsemaster.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import ru.hadron.morsemaster.R
import ru.hadron.morsemaster.db.StorageDao
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var storageDao: StorageDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
      //  Timber.e("---------------------------------------------${storageDao.hashCode()}")
        setSupportActionBar(toolbar)

        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->

              val actionBar = supportActionBar
                when (destination.id) {
                    R.id.settingsFragment -> {
                        actionBar?.show()
                        appBarLayout.setExpanded(true, true); //костыль
                        appBarLayout.setVisibility(View.VISIBLE)
                    }

                    R.id.morseFragment -> {
                        val orientation = resources.configuration.orientation
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            // In landscape
                            actionBar?.hide()
                            appBarLayout.setExpanded(false, false)
                            appBarLayout.setVisibility(View.GONE)
                        } else {
                            // In portrait
                            actionBar?.show()
                            appBarLayout.setExpanded(true, true); //костыль
                            appBarLayout.setVisibility(View.VISIBLE)
                        }
                    }
                }
            }

    }
}