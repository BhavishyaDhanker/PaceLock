package com.example.pacelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pacelock.Configuration.ConfigurationFragment
import com.example.pacelock.Home.HomeFragment
import com.example.pacelock.PastRuns.PastRunsFragment
import com.example.pacelock.Stats.StatsFragment
import com.example.pacelock.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel : HomeActViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)



        handleIncomingIntent()

        setupListeners()
        observeStates()
        }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIncomingIntent()
    }

    private fun handleIncomingIntent() {
        val open_frag = intent.getStringExtra("open_fragment")
        val runId = intent.getLongExtra("run_Id", -1)

        if(open_frag == "stats" && runId != -1L){

            binding.bottomNav.selectedItemId = R.id.stats


            replaceFrag(StatsFragment.newInstance(runId))
        }else{
            replaceFrag(HomeFragment())
        }
    }

    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.selectedTab.collect { tabId->
                when(tabId){
                    R.id.home -> replaceFrag(HomeFragment())

                    R.id.pastRuns -> replaceFrag(PastRunsFragment())

                    R.id.stats -> replaceFrag(StatsFragment())

                    R.id.config -> replaceFrag(ConfigurationFragment())
                }
            }
        }
    }

    private fun setupListeners() {
        binding.bottomNav.setOnItemSelectedListener {
            viewModel.changeTab(it.itemId)
            true
        }
    }


    private fun replaceFrag(frag: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.frame.id, frag) // here we wrote frame.id instead of frame because .replace wants an INT and not a frame
            .commit()
    }
}