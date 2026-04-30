package ni.edu.autocare

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import ni.edu.autocare.ui.theme.AutoCareTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// =======================================================
// MODELOS
// =======================================================

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
    val observaciones: String = "",
    val kilometraje: Int = 0,
    val fotoReciboUri: String? = null
)

// =======================================================
// STORAGE SIMPLE
// =======================================================

class AppStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autocare_db", Context.MODE_PRIVATE)

    fun guardarUsuarios(lista: List<Usuario>) {
        prefs.edit().putString("usuarios", encodeUsuarios(lista)).apply()
    }

    fun cargarUsuarios(): MutableList<Usuario> {
        val raw = prefs.getString("usuarios", "") ?: ""
        return decodeUsuarios(raw).toMutableList()
    }

    fun guardarServicios(lista: List<ServicioMantenimiento>) {
        prefs.edit().putString("servicios", encodeServicios(lista)).apply()
    }

    fun cargarServicios(): MutableList<ServicioMantenimiento> {
        val raw = prefs.getString("servicios", "") ?: ""
        return decodeServicios(raw).toMutableList()
    }

    private fun encodeUsuarios(lista: List<Usuario>): String {
        return lista.joinToString("|||") {
            listOf(
                it.id,
                it.nombre,
                it.correo,
                it.clave,
                it.fotoPerfilUri ?: "",
                it.fotoAutoUri ?: "",
                it.kilometrajeAuto.toString()
            ).joinToString(";;")
        }
    }

    private fun decodeUsuarios(raw: String): List<Usuario> {
        if (raw.isBlank()) return emptyList()

        return raw.split("|||").mapNotNull {
            val p = it.split(";;")

            try {
                Usuario(
                    id = p[0],
                    nombre = p[1],
                    correo = p[2],
                    clave = p[3],
                    fotoPerfilUri = p[4].ifBlank { null },
                    fotoAutoUri = p[5].ifBlank { null },
                    kilometrajeAuto = p[6].toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun encodeServicios(lista: List<ServicioMantenimiento>): String {
        return lista.joinToString("|||") {
            listOf(
                it.id,
                it.idUsuario,
                it.tipo,
                it.fecha.toString(),
                it.costo.toString(),
                it.observaciones,
                it.kilometraje.toString(),
                it.fotoReciboUri ?: ""
            ).joinToString(";;")
        }
    }

    private fun decodeServicios(raw: String): List<ServicioMantenimiento> {
        if (raw.isBlank()) return emptyList()

        return raw.split("|||").mapNotNull {
            val p = it.split(";;")

            try {
                ServicioMantenimiento(
                    id = p[0],
                    idUsuario = p[1],
                    tipo = p[2],
                    fecha = p[3].toLong(),
                    costo = p[4].toDouble(),
                    observaciones = p[5],
                    kilometraje = p[6].toInt(),
                    fotoReciboUri = p[7].ifBlank { null }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// =======================================================
// VIEWMODEL
// =======================================================

class AutoCareViewModel : ViewModel() {

    var darkMode by mutableStateOf(false)
    var usuarioActual by mutableStateOf<Usuario?>(null)

    val usuarios = mutableStateListOf<Usuario>()
    val servicios = mutableStateListOf<ServicioMantenimiento>()

    private lateinit var storage: AppStorage

    fun iniciarStorage(context: Context) {
        storage = AppStorage(context)

        if (usuarios.isEmpty()) {
            usuarios.addAll(storage.cargarUsuarios())
        }

        if (servicios.isEmpty()) {
            servicios.addAll(storage.cargarServicios())
        }
    }

    private fun guardarTodo() {
        if (::storage.isInitialized) {
            storage.guardarUsuarios(usuarios)
            storage.guardarServicios(servicios)
        }
    }

    fun registrar(nombre: String, correo: String, clave: String): String {
        if (nombre.isBlank()) {
            return "El nombre no puede estar vacío"
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            return "Correo inválido"
        }

        if (usuarios.any { it.correo == correo }) {
            return "Correo ya registrado"
        }

        if (clave.length < 6) {
            return "Contraseña débil"
        }

        usuarios.add(
            Usuario(
                nombre = nombre,
                correo = correo,
                clave = clave
            )
        )

        guardarTodo()
        return "OK"
    }

    fun login(correo: String, clave: String): Boolean {
        val user = usuarios.find {
            it.correo == correo && it.clave == clave
        }

        usuarioActual = user
        return user != null
    }

    fun cerrarSesion() {
        usuarioActual = null
    }

    val serviciosUsuario: List<ServicioMantenimiento>
        get() = servicios
            .filter { it.idUsuario == usuarioActual?.id }
            .sortedByDescending { it.fecha }

    fun guardarServicio(
        id: String?,
        tipo: String,
        fecha: Long,
        costo: Double,
        obs: String,
        km: Int,
        foto: String?
    ) {
        val user = usuarioActual ?: return

        if (tipo.isBlank()) return
        if (costo < 0) return
        if (km < 0) return

        if (id == null) {
            servicios.add(
                0,
                ServicioMantenimiento(
                    idUsuario = user.id,
                    tipo = tipo,
                    fecha = fecha,
                    costo = costo,
                    observaciones = obs,
                    kilometraje = km,
                    fotoReciboUri = foto
                )
            )
        } else {
            val index = servicios.indexOfFirst { it.id == id }

            if (index != -1) {
                servicios[index] = servicios[index].copy(
                    tipo = tipo,
                    fecha = fecha,
                    costo = costo,
                    observaciones = obs,
                    kilometraje = km,
                    fotoReciboUri = foto
                )
            }
        }

        actualizarKilometrajeUsuario(km)
        guardarTodo()
    }

    private fun actualizarKilometrajeUsuario(km: Int) {
        val user = usuarioActual ?: return

        val actualizado = user.copy(kilometrajeAuto = km)
        val index = usuarios.indexOfFirst { it.id == user.id }

        if (index != -1) {
            usuarios[index] = actualizado
        }

        usuarioActual = actualizado
    }

    fun eliminarServicio(id: String) {
        servicios.removeAll { it.id == id }
        guardarTodo()
    }

    fun actualizarPerfil(nombre: String, correo: String, clave: String, km: Int): String {
        val user = usuarioActual ?: return "Error"

        if (nombre.isBlank()) {
            return "El nombre no puede estar vacío"
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            return "Correo inválido"
        }

        if (usuarios.any { it.correo == correo && it.id != user.id }) {
            return "Correo ya usado"
        }

        if (clave.length < 6) {
            return "Contraseña débil"
        }

        if (km < 0) {
            return "Kilometraje inválido"
        }

        val actualizado = user.copy(
            nombre = nombre,
            correo = correo,
            clave = clave,
            kilometrajeAuto = km
        )

        val index = usuarios.indexOfFirst { it.id == user.id }

        if (index != -1) {
            usuarios[index] = actualizado
        }

        usuarioActual = actualizado
        guardarTodo()

        return "OK"
    }

    fun actualizarFotoPerfil(uri: String) {
        val user = usuarioActual ?: return

        val actualizado = user.copy(fotoPerfilUri = uri)
        val index = usuarios.indexOfFirst { it.id == user.id }

        if (index != -1) {
            usuarios[index] = actualizado
        }

        usuarioActual = actualizado
        guardarTodo()
    }

    fun actualizarFotoAuto(uri: String) {
        val user = usuarioActual ?: return

        val actualizado = user.copy(fotoAutoUri = uri)
        val index = usuarios.indexOfFirst { it.id == user.id }

        if (index != -1) {
            usuarios[index] = actualizado
        }

        usuarioActual = actualizado
        guardarTodo()
    }

    fun totalGastado(): Double {
        return serviciosUsuario.sumOf { it.costo }
    }

    fun promedio(): Double {
        return if (serviciosUsuario.isEmpty()) {
            0.0
        } else {
            totalGastado() / serviciosUsuario.size
        }
    }

    fun totalMesActual(): Double {
        val cal = Calendar.getInstance()
        val mes = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)

        return serviciosUsuario.filter {
            val c = Calendar.getInstance()
            c.timeInMillis = it.fecha

            c.get(Calendar.MONTH) == mes &&
                    c.get(Calendar.YEAR) == year
        }.sumOf { it.costo }
    }
}

// =======================================================
// MAIN ACTIVITY
// =======================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: AutoCareViewModel = viewModel()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                vm.iniciarStorage(context)
            }

            AutoCareTheme(darkTheme = vm.darkMode) {
                val nav = rememberNavController()

                NavHost(
                    navController = nav,
                    startDestination = "login"
                ) {
                    composable("login") {
                        PantallaLogin(nav, vm)
                    }

                    composable("registro") {
                        PantallaRegistro(nav, vm)
                    }

                    composable("inicio") {
                        PantallaInicio(nav, vm)
                    }

                    composable("perfil") {
                        PantallaPerfil(nav, vm)
                    }

                    composable(
                        route = "form?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                nullable = true
                                type = NavType.StringType
                                defaultValue = null
                            }
                        )
                    ) {
                        PantallaFormulario(
                            nav = nav,
                            vm = vm,
                            id = it.arguments?.getString("id")
                        )
                    }

                    composable(
                        route = "detalle/{id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.StringType
                            }
                        )
                    ) {
                        PantallaDetalle(
                            nav = nav,
                            vm = vm,
                            id = it.arguments?.getString("id") ?: ""
                        )
                    }
                }
            }
        }
    }
}

// =======================================================
// LOGIN
// =======================================================

@Composable
fun PantallaLogin(nav: NavHostController, vm: AutoCareViewModel) {
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Default.AutoMode,
                contentDescription = null,
                modifier = Modifier.padding(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "AutoCare",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Controla el mantenimiento de tu vehículo",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            isError = correo.isNotEmpty() &&
                    (!correo.contains("@") || !correo.contains(".")),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            visualTransformation =
                if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { visible = !visible }
                ) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (vm.login(correo, clave)) {
                    nav.navigate("inicio") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                } else {
                    Toast.makeText(
                        ctx,
                        "Credenciales incorrectas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "ENTRAR",
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = { nav.navigate("registro") }
        ) {
            Text("Crear cuenta nueva")
        }
    }
}

// =======================================================
// REGISTRO
// =======================================================

@Composable
fun PantallaRegistro(nav: NavHostController, vm: AutoCareViewModel) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        IconButton(
            onClick = { nav.popBackStack() }
        ) {
            Icon(
                Icons.Default.ArrowBackIosNew,
                contentDescription = null
            )
        }

        Text(
            text = "Crear cuenta",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Registra tus datos para comenzar a controlar el mantenimiento de tu auto.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            isError = correo.isNotEmpty() &&
                    (!correo.contains("@") || !correo.contains(".")),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            visualTransformation =
                if (claveVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { claveVisible = !claveVisible }
                ) {
                    Icon(
                        if (claveVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val r = vm.registrar(nombre, correo, clave)

                if (r == "OK") {
                    Toast.makeText(
                        ctx,
                        "Cuenta creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    nav.popBackStack()
                } else {
                    Toast.makeText(
                        ctx,
                        r,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            enabled = nombre.isNotBlank() &&
                    correo.isNotBlank() &&
                    clave.length >= 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "REGISTRARME",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =======================================================
// INICIO
// =======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(nav: NavHostController, vm: AutoCareViewModel) {
    val user = vm.usuarioActual ?: return

    var buscar by remember { mutableStateOf("") }
    var confirmarEliminar by remember { mutableStateOf<String?>(null) }

    val launcherFotoAuto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            vm.actualizarFotoAuto(it.toString())
        }
    }

    val lista = vm.serviciosUsuario.filter {
        it.tipo.contains(buscar, ignoreCase = true)
    }

    if (confirmarEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = null },
            title = { Text("Eliminar servicio") },
            text = { Text("¿Seguro que deseas eliminar este registro de mantenimiento?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.eliminarServicio(confirmarEliminar!!)
                        confirmarEliminar = null
                    }
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarEliminar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "MI GARAJE",
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(
                        onClick = { vm.darkMode = !vm.darkMode }
                    ) {
                        Icon(
                            if (vm.darkMode) Icons.Default.LightMode
                            else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null
                        )
                    },
                    label = { Text("Inicio") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("perfil") },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("form") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar servicio",
                    tint = Color.White
                )
            }
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clickable {
                        launcherFotoAuto.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box {
                    if (user.fotoAutoUri != null) {
                        AsyncImage(
                            model = user.fotoAutoUri,
                            contentDescription = "Foto del auto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Toca para subir foto de tu auto",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "Bienvenido de nuevo",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )

                        Text(
                            text = user.nombre,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Kilometraje actual: ${user.kilometrajeAuto} km",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Total", fontSize = 12.sp)

                        Text(
                            text = "$${vm.totalGastado()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Este mes", fontSize = 12.sp)

                        Text(
                            text = "$${vm.totalMesActual()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Promedio", fontSize = 12.sp)

                        Text(
                            text = "$${vm.promedio().roundToInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Historial de servicios",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = buscar,
                onValueChange = { buscar = it },
                label = { Text("Buscar servicio") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(14.dp))

            if (lista.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Todavía no hay servicios registrados",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Toca el botón + para agregar el primer mantenimiento.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lista.forEach { item ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    nav.navigate("detalle/${item.id}")
                                },
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    if (item.fotoReciboUri != null) {
                                        AsyncImage(
                                            model = item.fotoReciboUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.padding(13.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.tipo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )

                                    Text(
                                        text = SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                        ).format(Date(item.fecha)),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Text(
                                        text = "$${item.costo} • ${item.kilometraje} km",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        nav.navigate("form?id=${item.id}")
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        confirmarEliminar = item.id
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// FORMULARIO
// =======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(
    nav: NavHostController,
    vm: AutoCareViewModel,
    id: String?
) {
    val editar = vm.serviciosUsuario.find { it.id == id }
    val esEdicion = editar != null

    var tipo by remember {
        mutableStateOf(editar?.tipo ?: "Cambio de aceite")
    }

    var costo by remember {
        mutableStateOf(editar?.costo?.toString() ?: "")
    }

    var obs by remember {
        mutableStateOf(editar?.observaciones ?: "")
    }

    var km by remember {
        mutableStateOf(
            editar?.kilometraje?.toString()
                ?: vm.usuarioActual?.kilometrajeAuto.toString()
        )
    }

    var foto by remember {
        mutableStateOf<Uri?>(
            editar?.fotoReciboUri?.let { Uri.parse(it) }
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editar?.fecha ?: System.currentTimeMillis()
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            foto = uri
        }
    }

    val categorias = listOf(
        "Cambio de aceite",
        "Llantas",
        "Frenos",
        "Batería",
        "Lavado",
        "Revisión general"
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = { showPicker = false }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (esEdicion) "Editar servicio" else "Nuevo servicio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { nav.popBackStack() }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Text(
                text = "Datos del mantenimiento",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )

            Text(
                text = "Registra la información principal del servicio realizado.",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(18.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                tipo = categoria
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = datePickerState.selectedDateMillis?.let {
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(Date(it))
                } ?: "Fecha del servicio",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Fecha") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                    disabledLabelColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = costo,
                onValueChange = { costo = it },
                label = { Text("Costo total") },
                leadingIcon = {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = km,
                onValueChange = { km = it },
                label = { Text("Kilometraje") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = obs,
                onValueChange = { obs = it },
                label = { Text("Observaciones") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Evidencia",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        launcher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (foto != null) {
                    AsyncImage(
                        model = foto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(6.dp))

                        Text("Toca para añadir foto")
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val costoNum = costo.toDoubleOrNull()
                    val kmNum = km.toIntOrNull()
                    val fechaSeleccionada =
                        datePickerState.selectedDateMillis ?: System.currentTimeMillis()

                    if (costoNum == null || costoNum < 0) {
                        return@Button
                    }

                    if (kmNum == null || kmNum < 0) {
                        return@Button
                    }

                    vm.guardarServicio(
                        id = id,
                        tipo = tipo,
                        fecha = fechaSeleccionada,
                        costo = costoNum,
                        obs = obs,
                        km = kmNum,
                        foto = foto?.toString()
                    )

                    nav.popBackStack()
                },
                enabled = costo.isNotBlank() && km.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (esEdicion) "ACTUALIZAR SERVICIO"
                    else "GUARDAR SERVICIO",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =======================================================
// DETALLE
// =======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalle(
    nav: NavHostController,
    vm: AutoCareViewModel,
    id: String
) {
    val item = vm.serviciosUsuario.find { it.id == id } ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle del servicio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { nav.popBackStack() }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = item.tipo,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Fecha",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )

                    Text(
                        text = SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(Date(item.fecha)),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Costo",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "$${item.costo}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Kilometraje",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "${item.kilometraje} km",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Observaciones",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )

                    Text(
                        text = item.observaciones.ifBlank {
                            "Sin observaciones registradas."
                        }
                    )
                }
            }

            item.fotoReciboUri?.let {
                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Evidencia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(8.dp))

                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    nav.navigate("form?id=${item.id}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Editar servicio")
            }
        }
    }
}

// =======================================================
// PERFIL
// =======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    nav: NavHostController,
    vm: AutoCareViewModel
) {
    val u = vm.usuarioActual ?: return
    val ctx = LocalContext.current

    var nombre by remember { mutableStateOf(u.nombre) }
    var correo by remember { mutableStateOf(u.correo) }
    var clave by remember { mutableStateOf(u.clave) }
    var km by remember { mutableStateOf(u.kilometrajeAuto.toString()) }
    var claveVisible by remember { mutableStateOf(false) }

    val launcherPerfil = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            vm.actualizarFotoPerfil(it.toString())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "MI CUENTA",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { nav.popBackStack() }
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    modifier = Modifier
                        .size(130.dp)
                        .clickable {
                            launcherPerfil.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    if (u.fotoPerfilUri != null) {
                        AsyncImage(
                            model = u.fotoPerfilUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u.nombre.take(1).uppercase(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Información personal",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre completo") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null
                    )
                },
                isError = correo.isNotEmpty() &&
                        (!correo.contains("@") || !correo.contains(".")),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                visualTransformation =
                    if (claveVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { claveVisible = !claveVisible }
                    ) {
                        Icon(
                            if (claveVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = km,
                onValueChange = { km = it },
                label = { Text("Kilometraje actual") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val kmNum = km.toIntOrNull() ?: 0

                    val r = vm.actualizarPerfil(
                        nombre = nombre,
                        correo = correo,
                        clave = clave,
                        km = kmNum
                    )

                    if (r == "OK") {
                        Toast.makeText(
                            ctx,
                            "Perfil actualizado con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            ctx,
                            r,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "GUARDAR CAMBIOS",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = {
                    vm.cerrarSesion()

                    nav.navigate("login") {
                        popUpTo(0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("CERRAR SESIÓN")
            }
        }
    }
}