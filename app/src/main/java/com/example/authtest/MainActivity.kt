package com.example.authtest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.authtest.presenter.model.ImagePresent
import com.example.authtest.presenter.ImagesViewModel
import com.example.authtest.ui.theme.AuthTestTheme
import com.example.utils.Logger
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuthTestTheme {

                val imagesViewModel : ImagesViewModel = hiltViewModel()

                Logger.log("recomposing")

                val context = LocalContext.current

                val pickImageLauncher
                        = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
                    uri?.let {
                        imagesViewModel.addImage(uri)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ){ granted ->

                    if(granted){
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts
                                    .PickVisualMedia.ImageOnly
                            )
                        )
                    }
                }

                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE



                val images = imagesViewModel.imagesPagingFlow().collectAsLazyPagingItems()

                val systemUiController: SystemUiController = rememberSystemUiController()

                val lifecycleOwner = LocalLifecycleOwner.current

                val imageBitmap = remember {
                    mutableStateOf<Bitmap?>(null)
                }

                val currentImageIndex = remember {
                    mutableStateOf(-1)
                }



                DisposableEffect(key1 = lifecycleOwner){
                    val lifecycleObserver = LifecycleEventObserver { _, event ->

                        if(event == Lifecycle.Event.ON_RESUME){

                    systemUiController.isSystemBarsVisible = false
                    systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                    }
                }



                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)) {


                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(10.dp)
                    ){


                        items(count = images.itemCount, key = images.itemKey {
                            it.timestamp
                        }){index ->

                            images[index]?.let { image ->

                                Box(modifier = Modifier.animateItemPlacement()) {
                                    DrawImagePreview(
                                        image = image,
                                        onTap = {
                                            currentImageIndex.value = index
                                            imageBitmap.value = image.bitmap

                                                },
                                        onLongPress = (imagesViewModel::deleteImage)
                                    )
                                }
                            }
                        }
                    }


                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp),
                        shape = CircleShape,
                        onClick = {

                            val permissionRes = ContextCompat.checkSelfPermission(context, permission)

                            if(permissionRes != PackageManager.PERMISSION_GRANTED){
                                permissionLauncher.launch(permission)
                            } else {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts
                                            .PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        }
                    ) {

                        Icon(painter = painterResource(id = R.drawable.ic_add_image), contentDescription = "Add")

                    }

//                    val state = rememberPagerState(initialPage = currentImageIndex.value)
//
//                    Scaffold(Modifier.padding(0.dp)) {
//                        it
//                        HorizontalPager(pageCount = images.itemCount, state = state) {
//
//                        }
//                    }
//
//                    ZoomableBox(imageBitmap)

                    PagerView(currentImageIndex = currentImageIndex, images = images)
                }
            }
        }
    }
}


@Composable
fun DrawImagePreview(
    image : ImagePresent,
    onTap : () -> Unit,
    onLongPress : (ImagePresent) -> Unit
    ){
    val state = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    AnimatedVisibility(visibleState = state) {
        AsyncImage(
            model = image.bitmap,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(0.75f)
                .padding(5.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        onLongPress(image)
                    }, onTap = {
                        onTap()
                    })
                },
            contentScale = ContentScale.Crop

        )
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PagerView(
//    imageBitmap : MutableState<Bitmap?>,
    currentImageIndex : MutableState<Int>,
    images : LazyPagingItems<ImagePresent>
    ){


    val coroutine = rememberCoroutineScope()



    if(currentImageIndex.value >= 0){

        var isToScroll by remember{
            mutableStateOf(true)
        }

        var isToScaleIn by remember{
            mutableStateOf(false)
        }

        val scaleAnim by animateFloatAsState(if(isToScaleIn) 1f else 0f, label = "")


        val state = rememberPagerState(initialPage = currentImageIndex.value)
        Scaffold(containerColor = Color.Transparent) {
            HorizontalPager(
                userScrollEnabled = isToScroll,
                modifier = Modifier
                    .onGloballyPositioned {
                        isToScaleIn = true
                    }
                    .scale(scaleAnim)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->

//                            Logger.log("detected pager drag")

                        }

                    },pageCount = images.itemCount, state = state, contentPadding = it) {index ->

                ZoomableBox(
                    imageBitmap = images[index]?.bitmap,
                    onCollapse = { currentImageIndex.value = -1 },
//                    onPageChange = {shift ->
//                        val newPage = state.currentPage + shift
//                        if((0..images.itemCount).contains(newPage)){
////                            coroutine.launch {
////                                state.animateScrollToPage(newPage)
////                            }
//                        }
//                    },
                    onPagerScrollControl = {scrollEnabled ->
                        isToScroll = scrollEnabled
                    }
                )


            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableBox(
    imageBitmap : Bitmap?,
    minScale: Float = 1f,
    maxScale: Float = 3f,
    zoomScale : Float = 3f,
//    content: @Composable ZoomableBoxScope.() -> Unit
    onCollapse : () -> Unit,
//    onPageChange : (Int) -> Unit,
    onPagerScrollControl : (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }

    val scaleAnim by animateFloatAsState(targetValue = scale,
        finishedListener = {
        if(it == 0f)
            onCollapse()
    }, label = ""
    )


    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val offsetXAnim by animateFloatAsState(targetValue = offsetX, label = "")
    val offsetYAnim by animateFloatAsState(targetValue = offsetY, label = "")

    var boxGlobalSize by remember { mutableStateOf(IntSize.Zero) }


    val imageSize = remember() {
        mutableStateOf(IntSize.Zero)
    }


    var isToCollapse by remember {
        mutableStateOf(false)
    }

    var doubleTapOffset by remember{
        mutableStateOf(Offset(0f, 0f))
    }



    LaunchedEffect(isToCollapse){

        if(isToCollapse){
            scale = 0f
        }
    }


    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(
                    red = 0f,
                    green = 0f,
                    blue = 0f,
                    alpha = scaleAnim.coerceIn(0f..1f)
                )
            )
            .clip(RectangleShape)
            .onGloballyPositioned {
                boxGlobalSize = it.size
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {

                    if (scale == 1f) {
                        val maxX = (imageSize.value.width * (zoomScale - 1)) / 2
                        val tapX = (imageSize.value.width / 2 - doubleTapOffset.x) * zoomScale
                        val maxY =
                            if (boxGlobalSize.height >= (imageSize.value.height * zoomScale)) 0f
                            else (imageSize.value.height * zoomScale - boxGlobalSize.height) / 2
                        val tapY = ((imageSize.value.height) / 2 - doubleTapOffset.y) * zoomScale

                        scale = zoomScale
                        offsetX = maxOf(-maxX, minOf(maxX, tapX))
                        offsetY = maxOf(-maxY, minOf(maxY, tapY))
                        onPagerScrollControl(false)

                    } else {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        onPagerScrollControl(true)
                    }
                }
            )
            .pointerInput(Unit) {

                awaitEachGesture {
                    awaitFirstDown()

                    var maxX = 0f
                    var maxY = 0f
                    var isFling = false


                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()

                        if (zoom != 1f) {
                            onPagerScrollControl(false)
                        }

                        if (zoom < 1f && scale <= 1f) {
                            scale *= zoom
                            if (scale < 0.6f) {
                                isToCollapse = true
                            }

                        } else if (!isToCollapse) {

                            isFling = zoom == 1f

                            scale = maxOf(minScale, minOf(scale * zoom, maxScale))
                            maxX = (imageSize.value.width * (scale - 1)) / 2
                            offsetX = maxOf(-maxX, minOf(maxX, offsetX + pan.x))

                            maxY = if (boxGlobalSize.height >= (imageSize.value.height * scale)) 0f
                            else (imageSize.value.height * scale - boxGlobalSize.height) / 2
                            offsetY = maxOf(-maxY, minOf(maxY, offsetY + pan.y))
                        }

                    } while (event.changes.any { it.pressed })

                    if (scale < 1f && !isToCollapse) {
                        scale = 1f
                        onPagerScrollControl(true)
                    } else if (scale == 1f) {
                        onPagerScrollControl(true)
                    } else if (scale > 1f && isFling) {
                        val differenceX = offsetX - offsetXAnim
                        val differenceY = offsetY - offsetYAnim

                        if (abs(differenceX) > 150 || abs(differenceY) > 150) {

                            offsetX = maxOf(-maxX, minOf(maxX, offsetX + differenceX * scale / 2))
                            offsetY = maxOf(-maxY, minOf(maxY, offsetY + differenceY * scale / 2))
                        }
                    }
                }
            }


    ) {
//            val scope = ZoomableBoxScopeImpl(scaleAnim, offsetXAnim, offsetYAnim)
//            scope.content()


        AsyncImage(model = imageBitmap, contentDescription = null,
            modifier = Modifier
                .onSizeChanged {
                    imageSize.value = it
                }
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scaleAnim,
                    scaleY = scaleAnim,
                    translationX = offsetXAnim,
                    translationY = offsetYAnim
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val event = awaitPointerEvent()
                        val centroid = event.calculateCentroid()

                        if (centroid.isSpecified) {
                            doubleTapOffset = Offset(centroid.x, centroid.y)
                        }
                    }
                }
        )
    }

//    if(imageBitmap.value != null) {
//
//    }
}

//interface ZoomableBoxScope {
//    val scale: Float
//    val offsetX: Float
//    val offsetY: Float
//}
//
//private data class ZoomableBoxScopeImpl(
//    override val scale: Float,
//    override val offsetX: Float,
//    override val offsetY: Float
//) : ZoomableBoxScope



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImage(
    model: Uri?,
    contentDescription: String? = null,
    onBackHandler: () -> Unit,
) {
    val angle by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value
    val screenHeight = configuration.screenHeightDp.dp.value

    BackHandler { onBackHandler() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {
                    if (zoom > 1f) {
                        zoom = 1f
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        zoom = 3f
                    }
                }
            )
    ) {
        AsyncImage(
            model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    rotationZ = angle
                )
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1F..4F)
                            if (zoom > 1) {
                                val x = (pan.x * zoom)
                                val y = (pan.y * zoom)

                                val angleRad = angle * PI / 180.0

                                offsetX =
                                    (offsetX + (x * cos(angleRad) - y * sin(angleRad)).toFloat()).coerceIn(
                                        -(screenWidth * zoom)..(screenWidth * zoom)
                                    )
                                offsetY =
                                    (offsetY + (x * sin(angleRad) + y * cos(angleRad)).toFloat()).coerceIn(
                                        -(screenHeight * zoom)..(screenHeight * zoom)
                                    )


                            } else {
                                offsetX = 0F
                                offsetY = 0F
                            }
                        }
                    )
                }
                .fillMaxSize()
        )
        IconButton(
            onClick = onBackHandler
        ) {
            Icon(modifier = Modifier.size(18.dp),
                imageVector = Icons.Default.Close, // Replace  it
                contentDescription = "Close full screen",
                tint = Color.White
            )
        }
    }
}


