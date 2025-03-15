package com.mason.sendytestapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mason.sendytestapp.ui.theme.SendyTestAppTheme
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

        api = API.getInsatce(SERVER_URL, "sendy")
        viewModel.getTermOfUse(this, api)

        installSplashScreen()

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
    val activityContext = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = AppNavigationDestinations.loginScreen
    ) {
        composable(route = AppNavigationDestinations.loginScreen) {
            LoginScreen(
                otpSendStatus = vm.loginScreenFlow,
                onContinueClicked = { phoneNumber ->
                    vm.sendCode(
                        api = api,
                        phoneNumber = phoneNumber,
                        context = appContext
                    )
                },
                onCodeSent = {phone ->
                    navController.navigate("${AppNavigationDestinations.otpScreen}/${phone}")
                },
                onTermOfUseShow = {
                    vm.showTermOfUse(context = activityContext)
                }
            )
        }

        composable(route = "${AppNavigationDestinations.otpScreen}/{phone}") {
            val phone = it.arguments?.getString("phone") ?: ""
            Otp(
                otpConfirmStatus = vm.otpConfirmStatusFlow,
                otpResendStatus = vm.otpResendStatusFlow,
                phone = phone,
                onSucceedOtp = {
                    navController.navigate(AppNavigationDestinations.finalScreen) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                onContinueClicked = {otp->
                    vm.checkOtp(
                        context = appContext,
                        api = api,
                        otp = otp
                    )
                },
                onCodeResend = {phoneNumber ->
                    vm.resetOtp(
                        api = api,
                        phoneNumber = phoneNumber,
                        context = appContext
                    )
                }
            )
        }

        composable(route = AppNavigationDestinations.finalScreen) {
            FinalScreen()
        }

    }
}

