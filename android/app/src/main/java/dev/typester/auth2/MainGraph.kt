package dev.typester.auth2

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.typester.auth2.ui.screens.AddToken
import dev.typester.auth2.ui.screens.AddTokenScreen
import dev.typester.auth2.ui.screens.BiometricScreen
import dev.typester.auth2.ui.screens.DataMigrationScreen
import dev.typester.auth2.ui.screens.EncryptKeyScreen
import dev.typester.auth2.ui.screens.ListToken
import dev.typester.auth2.ui.screens.ListTokenScreen
import dev.typester.auth2.ui.screens.TokenDetail
import dev.typester.auth2.ui.screens.TokenDetailScreen
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val TAG = "MainGraph"

@Composable
fun RootGraph() {
    var migrationFinished by remember { mutableStateOf(false) }
    var keyIsAvailable by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    if (!migrationFinished) {
        Log.d(TAG, "show migration")
        DataMigrationScreen(
            navigateToMain = {
                migrationFinished = true
            }
        )
    } else if (!authenticated) {
        Log.d(TAG, "show miometric")
        BiometricScreen(
            onFinishAuthentication = {
                authenticated = true
            }
        )
    } else if (!keyIsAvailable) {
        Log.d(TAG, "show encryptkey")
        EncryptKeyScreen(
            onKeyIsAvailable = {
                keyIsAvailable = true
            }
        )
    } else {
        Log.d(TAG, "show main")
        MainGraph()
    }
}

@Composable
fun MainGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ListToken,
    ) {
        composable<ListToken> {
            ListTokenScreen(
                navigateToAddToken = {
                    navController.navigate(AddToken)
                },
                navigateToDetail = { id ->
                    navController.navigate(TokenDetail(id = id))
                }
            )
        }
        composable<TokenDetail> { backStackEntry ->
            val params: TokenDetail = backStackEntry.toRoute()
            TokenDetailScreen(
                id = params.id,
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable<AddToken> {
            AddTokenScreen(
                onBack = {
                    navController.popBackStack()
                },
                onAddToken = {
                    navController.popBackStack()
                }
            )
        }
    }
}

fun authenticateUser(
    context: Context,
    onAuthenticated: () -> Unit,
    onError: (String) -> Unit
) {
    val executor: Executor = Executors.newSingleThreadExecutor()
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Authenticate to continue")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}