package com.rheivalseptian8600.assesment3.ui.theme.screen

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rheivalseptian8600.assesment3.BuildConfig
import com.rheivalseptian8600.assesment3.R
import com.rheivalseptian8600.assesment3.model.Product
import com.rheivalseptian8600.assesment3.model.User
import com.rheivalseptian8600.assesment3.network.ProductApi
import com.rheivalseptian8600.assesment3.network.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dataStore = UserDataStore(context)
    val user by dataStore.userFlow.collectAsState(User())

    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val application = context.applicationContext as android.app.Application
                return MainViewModel(application) as T
            }
        }
    )
    val errorMessage by viewModel.errorMessage

    var showDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedProductForUpdate by remember { mutableStateOf<Product?>(null) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val launcher = rememberLauncherForActivityResult(CropImageContract()) {
        bitmap = getCroppedImage(context.contentResolver, it)
        if (bitmap != null) showProductDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Marketplace") },
                actions = {
                    IconButton(onClick = {
                        if (user.email.isEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch { signIn(context, dataStore) }
                        } else {
                            showDialog = true
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_account_circle_24),
                            contentDescription = stringResource(R.string.profil),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (user.email.isEmpty()) {
                    Toast.makeText(context, "Error: Anda belum login! Silakan login terlebih dahulu.", Toast.LENGTH_SHORT).show()
                } else {
                    val options = CropImageContractOptions(
                        null, CropImageOptions(
                            imageSourceIncludeGallery = true,
                            imageSourceIncludeCamera = true,
                            fixAspectRatio = true
                        )
                    )
                    launcher.launch(options)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.tambah_produk)
                )
            }
        }
    ) { innerPadding ->
        ScreenContent(
            viewModel = viewModel,
            userId = user.email,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onEditRequested = { product ->
                selectedProductForUpdate = product
                showUpdateDialog = true
            }
        )

        if (showDialog) {
            ProfilDialog(
                user = user,
                onDismissRequest = { showDialog = false }
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    signOut(context, dataStore)
                    viewModel.retrieveData("")
                }
                showDialog = false
            }
        }

        if (showProductDialog) {
            ProductDialog(
                bitmap = bitmap,
                onDismissRequest = { showProductDialog = false },
                onConfirmation = { title, description, price ->
                    if (user.email.isEmpty()) {
                        Toast.makeText(context, "Error: Sesi login tidak valid.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveData(user.email, title, description, price, bitmap!!)
                    }
                    showProductDialog = false
                }
            )
        }

        if (showUpdateDialog && selectedProductForUpdate != null) {
            var updatedTitle by remember { mutableStateOf(selectedProductForUpdate!!.title) }
            var updatedDesc by remember { mutableStateOf(selectedProductForUpdate!!.description) }
            var updatedPrice by remember { mutableStateOf(selectedProductForUpdate!!.price.toString()) }

            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Ubah Data Produk") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = updatedTitle,
                            onValueChange = { updatedTitle = it },
                            label = { Text("Nama Produk") }
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = updatedDesc,
                            onValueChange = { updatedDesc = it },
                            label = { Text("Deskripsi") }
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = updatedPrice,
                            onValueChange = { updatedPrice = it },
                            label = { Text("Harga") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateData(
                            id = selectedProductForUpdate!!.id ?: 0,
                            title = updatedTitle,
                            description = updatedDesc,
                            price = updatedPrice
                        )
                        showUpdateDialog = false
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }

    if (errorMessage != null) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        viewModel.clearMessage()
    }
}

private fun getCroppedImage(
    resolver: ContentResolver,
    result: CropImageView.CropResult
): Bitmap? {
    if (!result.isSuccessful) {
        Log.e("IMAGE", "Error: ${result.error}")
        return null
    }

    val uri = result.uriContent ?: return null

    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(resolver, uri)
    } else {
        val source = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(source)
    }
}

@Composable
fun ScreenContent(
    viewModel: MainViewModel,
    userId: String,
    modifier: Modifier = Modifier,
    onEditRequested: (Product) -> Unit
) {
    val data by viewModel.data
    val status by viewModel.status.collectAsState()

    LaunchedEffect(userId) {
        viewModel.retrieveData(userId)
    }

    when (status) {
        ProductApi.ApiStatus.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ProductApi.ApiStatus.SUCCESS -> {
            val filteredData = if (userId.isEmpty()) {
                data.filter { it.mine != "1" }
            } else {
                data
            }

            LazyVerticalGrid(
                modifier = modifier.fillMaxSize().padding(4.dp),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredData) { product ->
                    ListItem(
                        product = product,
                        onDelete = {
                            viewModel.deleteData(userId, product.id ?: 0)
                        },
                        onEdit = {
                            onEditRequested(product)
                        }
                    )
                }
            }
        }

        ProductApi.ApiStatus.FAILED -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.error))
                Button(
                    onClick = { viewModel.retrieveData(userId) },
                    modifier = Modifier.padding(top = 16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.try_again))
                }
            }
        }
    }
}

@Composable
fun ListItem(
    product: Product,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, Color.Gray)
            .clickable { if (product.mine == "1") onEdit() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(product.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.gambar, product.title),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.loading_img),
            error = painterResource(id = R.drawable.baseline_broken_image_24),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(4.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x80000000))
                .padding(8.dp)
        ) {
            Text(
                text = product.title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = "$${product.price}",
                fontStyle = FontStyle.Normal,
                fontSize = 14.sp,
                color = Color.Yellow
            )
        }

        if (product.mine == "1") {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).background(Color(0x4D000000)),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_edit),
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp).clickable { onEdit() }
                )
                Icon(
                    painter = painterResource(R.drawable.baseline_delete_24),
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.padding(4.dp).clickable { showDeleteDialog = true }
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus Produk") },
                text = { Text("Yakin ingin menghapus ${product.title} dari katalog?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

private suspend fun signIn(context: Context, dataStore: UserDataStore) {
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.API_KEY)
        .build()

    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        handleSignIn(result, dataStore)
    } catch (e: GetCredentialException) {
        Log.e("SIGN-IN", "Error: ${e.errorMessage}")
    }
}

private suspend fun handleSignIn(result: GetCredentialResponse, dataStore: UserDataStore) {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleId = GoogleIdTokenCredential.createFrom(credential.data)
            val nama = googleId.displayName ?: ""
            val email = googleId.id
            val photoUrl = googleId.profilePictureUri.toString()
            dataStore.saveData(User(nama, email, photoUrl))
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("SIGN-IN", "Error: ${e.message}")
        }
    } else {
        Log.e("SIGN-IN", "Error: unrecognized custom credential type.")
    }
}

private suspend fun signOut(context: Context, dataStore: UserDataStore) {
    try {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        dataStore.saveData(User())
    } catch (e: ClearCredentialException) {
        Log.e("SIGN-IN", "Error: ${e.errorMessage}")
    }
}