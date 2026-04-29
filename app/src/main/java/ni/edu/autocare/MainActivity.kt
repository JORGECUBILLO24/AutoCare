package ni.edu.autocare

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import ni.edu.autocare.ui.theme.AutoCareTheme
import java.text.SimpleDateFormat
import java.util.*

// --- 1. MODELOS DE DATOS ---

data class Usuario(
    val id: String = UUID.randomUUID().toString(),
    var nombre: String,
    var correo: String,
    var clave: String,
    var fotoPerfilUri: String? = null,
    var fotoAutoUri: String? = null,
    var kilometrajeAuto: Int = 0
)

data class ServicioMantenimiento(
    val id: String = UUID.randomUUID().toString(),
    val idUsuario: String,
    val tipo: String,
    val fecha: Long,
    val costo: Double,
    val fotoReciboUri: String? = null
)

// --- 2. VIEWMODEL (Lógica completa) ---

class AutoCareViewModel : ViewModel() {
    var estaEnModoOscuro by mutableStateOf(false)
    var usuarioLogeado by mutableStateOf<Usuario?>(null)

    val listaUsuarios = mutableStateListOf<Usuario>()
    val listaServiciosGlobal = mutableStateListOf<ServicioMantenimiento>()

    val serviciosDelUsuarioActual: List<ServicioMantenimiento>
        get() = listaServiciosGlobal.filter { it.idUsuario == usuarioLogeado?.id }.sortedByDescending { it.fecha }

    fun cambiarTema() { estaEnModoOscuro = !estaEnModoOscuro }

    fun registrarUsuario(nombre: String, correo: String, clave: String): Boolean {
        if (listaUsuarios.any { it.correo == correo }) return false
        listaUsuarios.add(Usuario(nombre = nombre, correo = correo, clave = clave))
        return true
    }

    fun iniciarSesion(correo: String, clave: String): Boolean {
        val usuario = listaUsuarios.find { it.correo == correo && it.clave == clave }
        usuarioLogeado = usuario
        return usuario != null
    }

    fun actualizarPerfil(nuevoNombre: String, nuevoCorreo: String, nuevaClave: String): Boolean {
        usuarioLogeado?.let { user ->
            if (nuevoCorreo != user.correo && listaUsuarios.any { it.correo == nuevoCorreo }) return false
            user.nombre = nuevoNombre
            user.correo = nuevoCorreo
            user.clave = nuevaClave
            usuarioLogeado = user.copy()
            return true
        }
        return false
    }

    // LÓGICA PARA CREAR O EDITAR UN SERVICIO
    fun guardarServicio(idServicio: String?, tipo: String, fecha: Long, costo: Double, uri: String?) {
        usuarioLogeado?.let { user ->
            if (idServicio != null) {
                // Editar existente
                val index = listaServiciosGlobal.indexOfFirst { it.id == idServicio }
                if (index != -1) {
                    listaServiciosGlobal[index] = listaServiciosGlobal[index].copy(
                        tipo = tipo, fecha = fecha, costo = costo, fotoReciboUri = uri
                    )
                }
            } else {
                // Crear nuevo
                listaServiciosGlobal.add(0, ServicioMantenimiento(
                    idUsuario = user.id, tipo = tipo, fecha = fecha, costo = costo, fotoReciboUri = uri
                ))
            }
        }
    }

    // LÓGICA PARA ELIMINAR
    fun eliminarServicio(idServicio: String) {
        listaServiciosGlobal.removeAll { it.id == idServicio }
    }

    fun actualizarFotoPerfil(uri: String) { usuarioLogeado = usuarioLogeado?.copy(fotoPerfilUri = uri) }
    fun actualizarFotoAuto(uri: String) { usuarioLogeado = usuarioLogeado?.copy(fotoAutoUri = uri) }
}

// --- 3. COMPONENTE PRINCIPAL ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AutoCareViewModel = viewModel()
            AutoCareTheme(darkTheme = vm.estaEnModoOscuro) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { VistaLogin(navController, vm) }
                    composable("registro") { VistaRegistro(navController, vm) }
                    composable("inicio") { VistaInicio(navController, vm) }
                    composable("perfil") { VistaPerfil(navController, vm) }

                    // RUTA DINÁMICA PARA EL FORMULARIO (NUEVO O EDITAR)
                    composable(
                        route = "formulario_servicio?servicioId={servicioId}",
                        arguments = listOf(navArgument("servicioId") { type = NavType.StringType; nullable = true })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("servicioId")
                        VistaFormularioServicio(navController, vm, id)
                    }
                }
            }
        }
    }
}

// --- 4. VISTAS DE AUTENTICACIÓN (LOGIN Y REGISTRO) ---
@Composable
fun VistaLogin(navController: NavHostController, vm: AutoCareViewModel) {
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(80.dp), CircleShape, MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Default.AutoMode, null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text("AutoCare", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = correo, onValueChange = { correo = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Email, null) },
            isError = correo.isNotEmpty() && !correo.contains("@")
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = clave, onValueChange = { clave = it },
            label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { claveVisible = !claveVisible }) {
                    Icon(if (claveVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            }
        )

        Spacer(Modifier.height(30.dp))
        Button(onClick = {
            if (vm.iniciarSesion(correo, clave)) navController.navigate("inicio") { popUpTo("login") { inclusive = true } }
            else Toast.makeText(contexto, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("ENTRAR", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { navController.navigate("registro") }) { Text("Crear cuenta nueva") }
    }
}

@Composable
fun VistaRegistro(navController: NavHostController, vm: AutoCareViewModel) {
    var nombre by remember { mutableStateOf("") }; var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }; var claveVisible by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBackIosNew, null) }
        Text("Registro", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), isError = correo.isNotEmpty() && !correo.contains("@"))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = clave, onValueChange = { clave = it }, label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { claveVisible = !claveVisible }) { Icon(if (claveVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }
        )
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = {
                if (vm.registrarUsuario(nombre, correo, clave)) {
                    Toast.makeText(contexto, "¡Registrado!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            },
            enabled = nombre.isNotEmpty() && correo.contains("@") && clave.length >= 6,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("REGISTRARME") }
    }
}

// --- 5. VISTA INICIO (HOME) CON BOTONES DE EDITAR Y ELIMINAR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaInicio(navController: NavHostController, vm: AutoCareViewModel) {
    val usuario = vm.usuarioLogeado ?: return
    val launcherFotoAuto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.actualizarFotoAuto(it.toString()) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("MI GARAJE", fontWeight = FontWeight.Black) },
                actions = { IconButton(onClick = { vm.cambiarTema() }) { Icon(if (vm.estaEnModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode, null) } })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = false, onClick = { navController.navigate("perfil") }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") })
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("formulario_servicio") }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().height(200.dp).clickable {
                launcherFotoAuto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Box {
                    if (usuario.fotoAutoUri != null) {
                        AsyncImage(model = usuario.fotoAutoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(48.dp))
                                Text("Toca para subir foto de tu auto")
                            }
                        }
                    }
                    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))).padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                        Text("Mantenimiento de", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Text(usuario.nombre, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Historial de Servicios", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                items(vm.serviciosDelUsuarioActual) { servicio ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), RoundedCornerShape(12.dp), MaterialTheme.colorScheme.secondaryContainer) {
                                if (servicio.fotoReciboUri != null) {
                                    AsyncImage(model = servicio.fotoReciboUri, contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Receipt, null, modifier = Modifier.padding(12.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(servicio.tipo, fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(servicio.fecha)), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$${servicio.costo}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            // BOTONES DE EDICIÓN Y ELIMINACIÓN
                            IconButton(onClick = { navController.navigate("formulario_servicio?servicioId=${servicio.id}") }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { vm.eliminarServicio(servicio.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 6. VISTA FORMULARIO (SIRVE PARA CREAR Y EDITAR) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaFormularioServicio(navController: NavHostController, vm: AutoCareViewModel, servicioId: String?) {
    // Buscar servicio si existe
    val servicioAEditar = remember(servicioId) { vm.serviciosDelUsuarioActual.find { it.id == servicioId } }
    val esEdicion = servicioAEditar != null

    var tipo by remember { mutableStateOf(servicioAEditar?.tipo ?: "") }
    var costo by remember { mutableStateOf(servicioAEditar?.costo?.toString() ?: "") }
    var fotoUri by remember { mutableStateOf<Uri?>(servicioAEditar?.fotoReciboUri?.let { Uri.parse(it) }) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = servicioAEditar?.fecha ?: System.currentTimeMillis())
    var showPicker by remember { mutableStateOf(false) }
    val launcherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) fotoUri = uri }

    if (showPicker) {
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton(onClick = { showPicker = false }) { Text("OK") } }) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (esEdicion) "Editar Gasto" else "Registrar Gasto") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = { Text("¿Qué servicio se realizó?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = datePickerState.selectedDateMillis?.let { SimpleDateFormat("dd/MM/yyyy").format(Date(it)) } ?: "Fecha del servicio",
                onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().clickable { showPicker = true }, enabled = false,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLeadingIconColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = costo, onValueChange = { costo = it }, label = { Text("Costo Total ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(24.dp))
            Text("Evidencia (Foto de recibo o auto)", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                launcherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, contentAlignment = Alignment.Center) {
                if (fotoUri != null) AsyncImage(model = fotoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null); Text(if (esEdicion) "Cambiar Foto" else "Añadir Foto", fontSize = 12.sp) }
            }

            Spacer(Modifier.height(40.dp))

            Button(onClick = {
                val fechaSegura = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val costoSeguro = costo.toDoubleOrNull() ?: 0.0
                val uriSegura = fotoUri?.toString()

                // Mando el ID si es edición, o null si es nuevo
                vm.guardarServicio(servicioId, tipo, fechaSegura, costoSeguro, uriSegura)
                navController.popBackStack()
            }, enabled = tipo.isNotEmpty() && costo.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (esEdicion) "ACTUALIZAR CAMBIOS" else "GUARDAR REGISTRO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 7. VISTA PERFIL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaPerfil(navController: NavHostController, vm: AutoCareViewModel) {
    val usuario = vm.usuarioLogeado ?: return
    var nombreEdit by remember { mutableStateOf(usuario.nombre) }
    var correoEdit by remember { mutableStateOf(usuario.correo) }
    var claveEdit by remember { mutableStateOf(usuario.clave) }
    var claveVisible by remember { mutableStateOf(false) }

    val launcherPerfil = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.actualizarFotoPerfil(it.toString()) }
    }
    val contexto = LocalContext.current

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("MI CUENTA", fontWeight = FontWeight.ExtraBold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBackIosNew, null) } })
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(modifier = Modifier.size(130.dp).clickable { launcherPerfil.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
                    if (usuario.fotoPerfilUri != null) {
                        AsyncImage(model = usuario.fotoPerfilUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text(usuario.nombre.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                    }
                }
                Surface(Modifier.size(36.dp), CircleShape, MaterialTheme.colorScheme.primary, shadowElevation = 2.dp) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(8.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Información Personal", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = nombreEdit, onValueChange = { nombreEdit = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Person, null) })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = correoEdit, onValueChange = { correoEdit = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Email, null) }, isError = correoEdit.isNotEmpty() && (!correoEdit.contains("@") || !correoEdit.contains(".")))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = claveEdit, onValueChange = { claveEdit = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { claveVisible = !claveVisible }) { Icon(if (claveVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (correoEdit.contains("@") && nombreEdit.isNotEmpty() && claveEdit.length >= 6) {
                        if (vm.actualizarPerfil(nombreEdit, correoEdit, claveEdit)) {
                            Toast.makeText(contexto, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(contexto, "El correo ya está en uso", Toast.LENGTH_SHORT).show()
                        }
                    } else { Toast.makeText(contexto, "Revisa los campos en rojo", Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = { navController.navigate("login") { popUpTo(0) } },
                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp))
                Text("CERRAR SESIÓN")
            }
        }
    }
}