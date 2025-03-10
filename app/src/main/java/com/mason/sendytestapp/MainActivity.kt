package com.mason.sendytestapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.plcoding.sendytestapp.ui.theme.SendyTestAppTheme
import land.sendy.pfe_sdk.activies.MasterActivity
import land.sendy.pfe_sdk.api.API
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : MasterActivity() {

    private val viewModel by viewModels<MainViewModel>()

    companion object {
        const val SERVER_URL = "https://testwallet.sendy.land/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        api = API.getInsatce(SERVER_URL, "sendy")

        setContent {
            SendyTestAppTheme {
                AppNavigation(vm = viewModel, api = api)
            }
        }
    }
}

object AppNavigationDestinations {
    const val loginScreen = "start"
    const val otpScreen = "otp"
    const val finalScreen = "final"
}

@Composable
fun AppNavigation(vm: MainViewModel, api: API) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext

    NavHost(
        navController = navController,
        startDestination = AppNavigationDestinations.loginScreen
    ) {
        composable(AppNavigationDestinations.loginScreen) {
            LoginScreen(
                navController = navController,
                loginScreenState = vm.loginScreenState,
                onContinueClicked = { phoneNumber ->
                    vm.sendCode(
                        api = api,
                        phoneNumber = phoneNumber,
                        context = appContext
                    )
                }
            )
        }

        composable(AppNavigationDestinations.otpScreen) {
            Otp(
                navController = navController,
                otpScreenState = vm.otpScreenState,
                onContinueClicked = {otp->
                    vm.checkOtp(
                        context = appContext,
                        api = api,
                        otp = otp
                    )
                }
            )
        }

        composable(AppNavigationDestinations.finalScreen) {
            FinalScreen()
        }

    }
}