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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import ni.edu.autocare.ui.theme.AutoCareTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
            } catch (e: Exception) { null }
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
            } catch (e: Exception) { null }
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

    lateinit var storage: AppStorage

    fun iniciarStorage(context: Context) {
        storage = AppStorage(context)

        if (usuarios.isEmpty()) usuarios.addAll(storage.cargarUsuarios())
        if (servicios.isEmpty()) servicios.addAll(storage.cargarServicios())
    }

    private fun guardarTodo() {
        storage.guardarUsuarios(usuarios)
        storage.guardarServicios(servicios)
    }

    fun registrar(nombre: String, correo: String, clave: String): String {
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches())
            return "Correo inválido"

        if (usuarios.any { it.correo == correo })
            return "Correo ya registrado"

        if (clave.length < 6)
            return "Contraseña débil"

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
        if (costo < 0) return

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

        usuarioActual = usuarioActual?.copy(kilometrajeAuto = km)
        guardarTodo()
    }

    fun eliminarServicio(id: String) {
        servicios.removeAll { it.id == id }
        guardarTodo()
    }

    fun actualizarPerfil(nombre: String, correo: String, clave: String, km: Int): String {
        val u = usuarioActual ?: return "Error"

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches())
            return "Correo inválido"

        if (usuarios.any { it.correo == correo && it.id != u.id })
            return "Correo ya usado"

        u.nombre = nombre
        u.correo = correo
        u.clave = clave
        u.kilometrajeAuto = km

        usuarioActual = u.copy()
        guardarTodo()
        return "OK"
    }

    fun totalGastado(): Double = serviciosUsuario.sumOf { it.costo }

    fun promedio(): Double =
        if (serviciosUsuario.isEmpty()) 0.0
        else totalGastado() / serviciosUsuario.size

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
// MAIN
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

                NavHost(navController = nav, startDestination = "login") {

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
                        "form?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                nullable = true
                                type = NavType.StringType
                            }
                        )
                    ) {
                        PantallaFormulario(nav, vm,
                            it.arguments?.getString("id"))
                    }

                    composable(
                        "detalle/{id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.StringType
                            }
                        )
                    ) {
                        PantallaDetalle(nav, vm,
                            it.arguments?.getString("id") ?: "")
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
        Modifier.fillMaxSize().padding(30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("AutoCare", fontSize = 34.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(30.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation =
                if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(Icons.Default.Visibility, null)
                }
            }
        )

        Spacer(Modifier.height(25.dp))

        Button(
            onClick = {
                if (vm.login(correo, clave)) {
                    nav.navigate("inicio") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Toast.makeText(
                        ctx,
                        "Credenciales incorrectas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }

        TextButton(
            onClick = { nav.navigate("registro") }
        ) {
            Text("Crear cuenta")
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

    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(25.dp)
    ) {
        Text("Registro", fontSize = 30.sp)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(correo, { correo = it }, label = { Text("Correo") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(clave, { clave = it }, label = { Text("Clave") })

        Spacer(Modifier.height(25.dp))

        Button(
            onClick = {
                val r = vm.registrar(nombre, correo, clave)

                if (r == "OK") {
                    Toast.makeText(ctx, "Registrado", Toast.LENGTH_SHORT).show()
                    nav.popBackStack()
                } else {
                    Toast.makeText(ctx, r, Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Registrar")
        }
    }
}

// =======================================================
// INICIO
// =======================================================

@Composable
fun PantallaInicio(nav: NavHostController, vm: AutoCareViewModel) {

    val user = vm.usuarioActual ?: return
    var buscar by remember { mutableStateOf("") }
    var confirmarEliminar by remember { mutableStateOf<String?>(null) }

    val lista = vm.serviciosUsuario.filter {
        it.tipo.contains(buscar, true)
    }

    if (confirmarEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = null },
            title = { Text("Eliminar") },
            text = { Text("¿Seguro de eliminar este servicio?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarServicio(confirmarEliminar!!)
                    confirmarEliminar = null
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmarEliminar = null
                }) { Text("No") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("form") }
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(16.dp)
        ) {

            Text(
                "Hola ${user.nombre}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text("Kilometraje: ${user.kilometrajeAuto} km")

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen")
                    Text("Total: $${vm.totalGastado()}")
                    Text("Mes: $${vm.totalMesActual()}")
                    Text("Promedio: $${vm.promedio().roundToInt()}")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = buscar,
                onValueChange = { buscar = it },
                label = { Text("Buscar servicio") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Button(onClick = {
                    nav.navigate("perfil")
                }) {
                    Text("Perfil")
                }

                Spacer(Modifier.width(10.dp))

                Button(onClick = {
                    vm.darkMode = !vm.darkMode
                }) {
                    Text("Tema")
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(lista) { item ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                nav.navigate("detalle/${item.id}")
                            }
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(Modifier.weight(1f)) {
                                Text(item.tipo, fontWeight = FontWeight.Bold)
                                Text(
                                    SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        Locale.getDefault()
                                    ).format(Date(item.fecha))
                                )
                                Text("$${item.costo}")
                            }

                            IconButton(
                                onClick = {
                                    nav.navigate("form?id=${item.id}")
                                }
                            ) {
                                Icon(Icons.Default.Edit, null)
                            }

                            IconButton(
                                onClick = {
                                    confirmarEliminar = item.id
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = Color.Red
                                )
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) {
        if (it != null) foto = it
    }

    val categorias = listOf(
        "Cambio de aceite",
        "Llantas",
        "Frenos",
        "Batería",
        "Lavado",
        "Revisión general"
    )

    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            if (editar == null) "Nuevo Servicio"
            else "Editar Servicio",
            fontSize = 28.sp
        )

        Spacer(Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            OutlinedTextField(
                value = tipo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categorias.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            tipo = it
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            costo,
            { costo = it },
            label = { Text("Costo") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            km,
            { km = it },
            label = { Text("Kilometraje") }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            obs,
            { obs = it },
            label = { Text("Observaciones") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                launcher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts
                            .PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text("Seleccionar Foto")
        }

        foto?.let {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val costoNum = costo.toDoubleOrNull() ?: -1.0
                val kmNum = km.toIntOrNull() ?: 0

                if (costoNum < 0) return@Button

                vm.guardarServicio(
                    id,
                    tipo,
                    System.currentTimeMillis(),
                    costoNum,
                    obs,
                    kmNum,
                    foto?.toString()
                )

                nav.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar")
        }
    }
}

// =======================================================
// DETALLE
// =======================================================

@Composable
fun PantallaDetalle(
    nav: NavHostController,
    vm: AutoCareViewModel,
    id: String
) {

    val item = vm.serviciosUsuario.find { it.id == id } ?: return

    Column(
        Modifier.fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(item.tipo, fontSize = 30.sp)

        Spacer(Modifier.height(15.dp))

        Text("Fecha:")
        Text(
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date(item.fecha))
        )

        Spacer(Modifier.height(10.dp))

        Text("Costo: $${item.costo}")
        Text("Kilometraje: ${item.kilometraje} km")

        Spacer(Modifier.height(10.dp))

        Text("Observaciones:")
        Text(item.observaciones)

        item.fotoReciboUri?.let {
            Spacer(Modifier.height(16.dp))
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { nav.popBackStack() }
        ) {
            Text("Volver")
        }
    }
}

// =======================================================
// PERFIL
// =======================================================

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

    Column(
        Modifier.fillMaxSize().padding(20.dp)
    ) {

        Text("Perfil", fontSize = 30.sp)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(correo, { correo = it }, label = { Text("Correo") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(clave, { clave = it }, label = { Text("Clave") })
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(km, { km = it }, label = { Text("Kilometraje") })

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val r = vm.actualizarPerfil(
                    nombre,
                    correo,
                    clave,
                    km.toIntOrNull() ?: 0
                )

                Toast.makeText(ctx, r, Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("Guardar")
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                vm.cerrarSesion()
                nav.navigate("login") {
                    popUpTo(0)
                }
            }
        ) {
            Text("Cerrar sesión")
        }
    }
}