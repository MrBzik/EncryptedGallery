package com.example.authtest

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.authtest.ui.theme.AuthTestTheme
import com.example.utils.Constants.PATTERN_CONFIRM
import com.example.utils.Constants.PATTERN_CREATE
import com.example.utils.Constants.PATTERN_ENTER

import com.example.utils.Constants.PATTERN_PREF
import com.example.utils.Constants.PATTERN_SUCCESS
import com.example.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.StringBuilder

class AuthActivity : FragmentActivity() {

    data class LineShadow(val start : Offset, val end : Offset, val alpha : Animatable<Float, AnimationVector1D>)


    val authPref by lazy {

        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    }

    private fun getBiometricPrompt() : BiometricPrompt{

        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.d("CHECKTAGS", "error")
                super.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d("CHECKTAGS", "success")
                super.onAuthenticationSucceeded(result)
                val intent = Intent(this@AuthActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this@AuthActivity.startActivity(intent)
            }

            override fun onAuthenticationFailed() {
                Log.d("CHECKTAGS", "failed")
                super.onAuthenticationFailed()
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Bio Auth")
        .setDescription("testing bio auth")
        .setNegativeButtonText("Cancel")
        .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            AuthTestTheme {

                var enteredPinBuff by remember {
                    mutableStateOf("")
                }
                var enteredPinConfirm by remember{
                    mutableStateOf("")
                }

                var authPatternState by remember {
                    mutableStateOf(
                        if(authPref.getString(PATTERN_PREF, "") == "")
                            PATTERN_CREATE
                        else PATTERN_ENTER
                    )
                }


                var title by remember {
                    mutableStateOf("")
                }


                val density = LocalDensity.current.density

                val nodeScale = remember {
                    Animatable(1f)
                }

                val iconSize = remember {
                    24 * density
                }
                val iconHalfSize = remember {
                    12 * density
                }

                val firstRange = remember {
                    0f..iconSize
                }
                var secondRange = remember {
                    0f..2f
                }
                var thirdRange = remember {
                    0f..2f
                }

                var size by remember {
                    mutableStateOf(0)
                }

                var lineStartOffset by remember {
                    mutableStateOf<Offset?>(null)
                }

                var lineEndOffset by remember {
                    mutableStateOf<Offset?>(null)
                }

                var isToDrawLine by remember {
                    mutableStateOf(false)
                }

                var newNodePosition by remember {
                    mutableStateOf(0)
                }

                val enteredPin by remember {
                    mutableStateOf(IntArray(10))
                }


                val canvasState = remember {
                    ArrayList<LineShadow>()
                }

                val coroutine = rememberCoroutineScope()
                val context = LocalContext.current


                fun restoreState(){
                    Logger.log("restoring state")
                    if(authPatternState != PATTERN_CONFIRM)
                        enteredPinBuff = ""
                    enteredPinConfirm = ""
                    canvasState.clear()
                    lineStartOffset = null
                    lineEndOffset = null
                    for (i in enteredPin.indices) {
                        enteredPin[i] = 0
                    }
                    isToDrawLine = false
                }

                fun checkEnteredPattern(){

                    Logger.log("entered: $enteredPinBuff")

                    if(enteredPinBuff.length < 2)
                        return

                    when(authPatternState){

                        PATTERN_CREATE -> {
                            authPatternState = PATTERN_CONFIRM
                        }
                        PATTERN_CONFIRM-> {
                            if(enteredPinConfirm == enteredPinBuff){
                                authPref.edit().putString(PATTERN_PREF, enteredPinBuff).apply()
                                authPatternState = PATTERN_SUCCESS
                            } else {
                                Toast.makeText(context, "Does not match", Toast.LENGTH_SHORT).show()
                                authPatternState = PATTERN_CREATE
                            }
                        }
                        PATTERN_ENTER ->{
                            if(enteredPinBuff == authPref.getString(PATTERN_PREF, "842141424")){
                                authPatternState = PATTERN_SUCCESS
                            } else {
                                Toast.makeText(context, "Wrong pattern", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }


                LaunchedEffect(key1 = authPatternState){

                    Logger.log("auth state: $authPatternState")

                    when(authPatternState){
                        PATTERN_CREATE -> title = "Create security pattern"
                        PATTERN_CONFIRM -> title = "Confirm security pattern"
                        PATTERN_ENTER -> title = "Enter security pattern"
                        PATTERN_SUCCESS -> getBiometricPrompt().authenticate(promptInfo)
                    }
                }

                LaunchedEffect(newNodePosition){

                    if(newNodePosition > 0 && enteredPin[newNodePosition] == 0){

                        Logger.log("entering : $newNodePosition")

                        if(authPatternState == PATTERN_CONFIRM){
                            enteredPinConfirm += newNodePosition
                        } else {
                            enteredPinBuff += newNodePosition
                        }


                        coroutine.launch {
                            nodeScale.animateTo(2.5f)
                            nodeScale.animateTo(1f)
                            if(!isToDrawLine)
                                newNodePosition = 0
                        }


                        val start = lineStartOffset
                        val newStart = when(newNodePosition){
                            1 -> {
                                Offset(0f + iconHalfSize, 0f + iconHalfSize)
                            }
                            2 -> {
                                Offset(0f + iconHalfSize, (size / 2).toFloat())
                            }
                            3 -> {
                                Offset(0f + iconHalfSize, size - iconHalfSize)
                            }
                            4 -> {
                                Offset((size / 2).toFloat(), 0f + iconHalfSize)
                            }
                            5 -> {
                                Offset((size / 2).toFloat(), (size / 2).toFloat())
                            }
                            6 -> {
                                Offset((size / 2).toFloat(), size - iconHalfSize)
                            }
                            7 -> {
                                Offset(size - iconHalfSize, 0f + iconHalfSize)
                            }
                            8 -> {
                                Offset(size - iconHalfSize, (size /2).toFloat())
                            }
                            9 -> {
                                Offset(size - iconHalfSize, size - iconHalfSize)
                            }
                            else -> Offset(0f, 0f)
                        }

                        start?.let {

                            coroutine.launch {
                                val shadow = LineShadow(start, newStart, Animatable(1f))

                                canvasState.add(shadow)

                                shadow.alpha.animateTo(0f, animationSpec = tween(800))

                                canvasState.remove(shadow)
                            }
                        }

                        lineStartOffset = newStart
                        lineEndOffset = newStart

                        enteredPin[newNodePosition] = 1

                        if(!isToDrawLine)
                            isToDrawLine = true

                    }

                }


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Spacer(modifier = Modifier.height(64.dp))

                    Text(text = title, color = Color.DarkGray, fontSize = 20.sp)

                    Spacer(modifier = Modifier.height(32.dp))


                    BoxWithConstraints(modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize()
                        .padding(64.dp)
                        .onGloballyPositioned {
                            size = it.size.height
                            secondRange = (size / 2 - iconHalfSize)..(size / 2 + iconHalfSize)
                            thirdRange = (size - iconSize)..size.toFloat()
                        }
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->


                                    if (isToDrawLine) {

                                        lineEndOffset = Offset(
                                            change.position.x,
                                            change.position.y
                                        )

                                        if (
                                            (0..size).contains(change.position.x.toInt()) &&
                                            (0..size).contains(change.position.y.toInt())

                                        ) {
                                            val xPos = if (firstRange.contains(change.position.x)) 1
                                            else if (secondRange.contains(change.position.x)) 2
                                            else if (thirdRange.contains(change.position.x)) 3
                                            else 0

                                            if (xPos != 0) {
                                                val yPos =
                                                    if (firstRange.contains(change.position.y)) 1
                                                    else if (secondRange.contains(change.position.y)) 2
                                                    else if (thirdRange.contains(change.position.y)) 3
                                                    else 0


                                                if (yPos != 0) {

                                                    when (xPos) {
                                                        1 -> newNodePosition = when (yPos) {
                                                            1 -> 1
                                                            2 -> 2
                                                            else -> 3
                                                        }

                                                        2 -> newNodePosition = when (yPos) {
                                                            1 -> 4
                                                            2 -> 5
                                                            else -> 6
                                                        }

                                                        3 -> newNodePosition = when (yPos) {
                                                            1 -> 7
                                                            2 -> 8
                                                            else -> 9
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    checkEnteredPattern()
                                    restoreState()
                                    newNodePosition = 0
                                }
                            )
                        }
                    )  {


                        DrawNode(modifier = Modifier
                            .align(Alignment.TopStart)
                            .scale(if (newNodePosition == 1) nodeScale.value else 1f),
                            onClick = { newNodePosition = 1 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 2) nodeScale.value else 1f)
                            .align(Alignment.CenterStart),
                            onClick = { newNodePosition = 2 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 3) nodeScale.value else 1f)
                            .align(Alignment.BottomStart),
                            onClick = { newNodePosition = 3 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 4) nodeScale.value else 1f)
                            .align(Alignment.TopCenter),
                            onClick = { newNodePosition = 4 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 5) nodeScale.value else 1f)
                            .align(Alignment.Center),
                            onClick = { newNodePosition = 5 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 6) nodeScale.value else 1f)
                            .align(Alignment.BottomCenter),
                            onClick = { newNodePosition = 6 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 7) nodeScale.value else 1f)
                            .align(Alignment.TopEnd),
                            onClick = { newNodePosition = 7 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 8) nodeScale.value else 1f)
                            .align(Alignment.CenterEnd),
                            onClick = { newNodePosition = 8 },
                            onActionUp = { restoreState() })

                        DrawNode(modifier = Modifier
                            .scale(if (newNodePosition == 9) nodeScale.value else 1f)
                            .align(Alignment.BottomEnd),
                            onClick = { newNodePosition = 9},
                            onActionUp = { restoreState() })


                        if(isToDrawLine)
                            Canvas(modifier = Modifier.fillMaxSize()){

                                if(lineStartOffset != null && lineEndOffset != null)
                                    drawLine(lineStartOffset!!, lineEndOffset!!)

                                canvasState.forEach { lineShadow ->

                                    drawShadow(start = lineShadow.start, end = lineShadow.end, alpha = lineShadow.alpha.value)
                                }

                            }

                    }
                }
            }
        }
    }
}



fun DrawScope.drawShadow(
    start: Offset,
    end: Offset,
    alpha : Float
){
    drawLine(
        color = Color.LightGray,
        start = start,
        end = end,
        strokeWidth = 6f * density,
        cap = StrokeCap.Round,
        alpha = alpha
    )
}



fun DrawScope.drawLine(start : Offset, end : Offset){

    drawLine(
        color = Color.LightGray,
        start = start,
        end = end,
        strokeWidth = 6f * density,
        cap = StrokeCap.Round
    )

}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawNode(
    modifier: Modifier,
    onClick : () -> Unit = {},
    onActionUp : () -> Unit = {}
){

    Icon(
        painter = painterResource(id = R.drawable.ic_circle),
        contentDescription = null,
        tint = Color.LightGray,
        modifier = modifier
            .pointerInteropFilter {
                if (it.action == MotionEvent.ACTION_UP)
                    onActionUp()
                else if (it.action == MotionEvent.ACTION_DOWN) {
                    onClick()
                }
                true
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onClick()
                })
            }
    )
}

