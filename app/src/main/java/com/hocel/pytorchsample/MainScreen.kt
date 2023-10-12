package com.hocel.pytorchsample

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val bitmap by remember {
        mainViewModel.mBitmap
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (bitmap != null) {
            Image(
                modifier = Modifier
                    .padding(8.dp)
                    .size(500.dp),
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = ""
            )
            Button(onClick = { mainViewModel.run() }) {
                Text(text = "Segment")
            }
        } else {
            Text(text = "Image is null")
        }
        Button(onClick = { mainViewModel.loadImage() }) {
            Text(text = "Load image")
        }
    }
}