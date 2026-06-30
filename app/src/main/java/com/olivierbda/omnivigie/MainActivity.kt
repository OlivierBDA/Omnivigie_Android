package com.olivierbda.omnivigie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.olivierbda.omnivigie.ui.HomeScreen
import com.olivierbda.omnivigie.ui.theme.OmnivigieTheme
import com.olivierbda.omnivigie.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: HomeViewModel by viewModels()

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.onAuthorizationResult(this, result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmnivigieTheme {
                val authIntent by viewModel.authorizationPendingIntent.collectAsState()
                
                LaunchedEffect(authIntent) {
                    authIntent?.let {
                        authLauncher.launch(
                            IntentSenderRequest.Builder(it.intentSender).build()
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
