package ni.edu.autocare

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.UUID
import ni.edu.autocare.ui.theme.AutoCareTheme

// --- 1. MODELOS DE DATOS (POO) ---
data class Vehiculo(
    var marca: String = "Toyota",
    var modelo: String = "Corolla",
    var kilometraje: Int = 50000
)

data class Servicio(
    val id: String = UUID.randomUUID().toString(),
    val tipoServicio: String,
    val fecha: String,
    val costo: Double,
    val notas: String,
    val imagenUri: String? = null // Nuevo: Soporte para imagen
)

// --- 2. VIEWMODEL (Gestión de estado centralizada) ---
class AutoCareViewModel : ViewModel() {
    var vehiculo by mutableStateOf(Vehiculo())
        private set

    val servicios = mutableStateListOf<Servicio>()

    fun actualizarKm(nuevoKm: Int) { vehiculo = vehiculo.copy(kilometraje = nuevoKm) }

    fun guardarServicio(servicio: Servicio) {
        val index = servicios.indexOfFirst { it.id == servicio.id }
        if (index != -1) {
            servicios[index] = servicio // Editar
        } else {
            servicios.add(servicio) // Agregar nuevo
        }
    }

    fun eliminarServicio(id: String) { servicios.removeAll { it.id == id } }
}

// --- 3. ACTIVIDAD PRINCIPAL ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Adaptable a Modo Oscuro
                ) {
                    AutoCareApp()
                }
            }
        }
    }
}

// --- 4. NAVEGACIÓN Y ANIMACIONES ---
@Composable
fun AutoCareApp(viewModel: AutoCareViewModel = viewModel()) {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            // Animaciones globales de transición
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { 1000 }) },
            exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -1000 }) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -1000 }) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { 1000 }) }
        ) {
            composable("home") { HomePantalla(navController, viewModel) }

            // Reutilizamos la pantalla para Agregar y Editar
            composable(
                route = "formulario?servicioId={servicioId}",
                arguments = listOf(navArgument("servicioId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("servicioId")
                FormularioServicioPantalla(navController, viewModel, id)
            }

            composable("detalle/{servicioId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("servicioId")
                DetalleServicioPantalla(navController, viewModel, id)
            }

            composable("actualizar_km") { ActualizarKmPantalla(navController, viewModel) }
        }
    }
}

// --- 5. PANTALLAS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePantalla(navController: NavHostController, viewModel: AutoCareViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Garaje", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("formulario") },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.onSecondary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Tarjeta Superior Animada
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = "Auto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${viewModel.vehiculo.marca} ${viewModel.vehiculo.modelo}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Speed, contentDescription = "Kilometraje", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${viewModel.vehiculo.kilometraje} km", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate("actualizar_km") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar Kilometraje")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Historial de Mantenimiento", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.servicios.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay servicios registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.servicios) { servicio ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("detalle/${servicio.id}") },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Muestra un icono genérico o la miniatura de la imagen si existe
                                if (servicio.imagenUri != null) {
                                    AsyncImage(
                                        model = servicio.imagenUri,
                                        contentDescription = "Foto servicio",
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Filled.Build, contentDescription = "Servicio", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(servicio.tipoServicio, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(servicio.fecha, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text("$${servicio.costo}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioServicioPantalla(navController: NavHostController, viewModel: AutoCareViewModel, servicioId: String?) {
    // Si hay un ID, buscamos el servicio para Editar, si no, es Nuevo.
    val servicioAEditar = viewModel.servicios.find { it.id == servicioId }
    val esEdicion = servicioAEditar != null

    var tipo by remember { mutableStateOf(servicioAEditar?.tipoServicio ?: "") }
    var fecha by remember { mutableStateOf(servicioAEditar?.fecha ?: "") }
    var costo by remember { mutableStateOf(servicioAEditar?.costo?.toString() ?: "") }
    var notas by remember { mutableStateOf(servicioAEditar?.notas ?: "") }
    var imagenUri by remember { mutableStateOf<Uri?>(servicioAEditar?.imagenUri?.let { Uri.parse(it) }) }

    // Lanzador para abrir la galería y seleccionar imagen
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) imagenUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar Servicio" else "Nuevo Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = { Text("Tipo (ej. Aceite, Frenos)") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Build, null) })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = costo, onValueChange = { costo = it }, label = { Text("Costo ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.AttachMoney, null) })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("Notas adicionales") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Notes, null) })

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para agregar/cambiar imagen
            OutlinedButton(
                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (imagenUri == null) "Adjuntar Imagen (Factura/Daño)" else "Cambiar Imagen")
            }

            // Vista previa de la imagen
            if (imagenUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = imagenUri,
                    contentDescription = "Vista previa",
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (tipo.isNotBlank() && costo.isNotBlank()) {
                        val nuevoServicio = Servicio(
                            id = servicioAEditar?.id ?: UUID.randomUUID().toString(), // Mantiene ID si es edición
                            tipoServicio = tipo,
                            fecha = fecha,
                            costo = costo.toDoubleOrNull() ?: 0.0,
                            notas = notas,
                            imagenUri = imagenUri?.toString()
                        )
                        viewModel.guardarServicio(nuevoServicio)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (esEdicion) "Actualizar Cambios" else "Guardar Servicio", fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleServicioPantalla(navController: NavHostController, viewModel: AutoCareViewModel, servicioId: String?) {
    val servicio = viewModel.servicios.find { it.id == servicioId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    // Botón Editar
                    IconButton(onClick = { navController.navigate("formulario?servicioId=${servicio?.id}") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    // Botón Eliminar
                    IconButton(onClick = {
                        servicio?.id?.let {
                            viewModel.eliminarServicio(it)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
            if (servicio != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(servicio.tipoServicio, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, null); Spacer(modifier = Modifier.width(8.dp))
                            Text(servicio.fecha, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AttachMoney, null); Spacer(modifier = Modifier.width(8.dp))
                            Text("${servicio.costo}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Notas:", fontWeight = FontWeight.Bold)
                        Text(servicio.notas, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                if (servicio.imagenUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Evidencia:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = Uri.parse(servicio.imagenUri),
                        contentDescription = "Imagen de evidencia",
                        modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualizarKmPantalla(navController: NavHostController, viewModel: AutoCareViewModel) {
    var nuevoKm by remember { mutableStateOf(viewModel.vehiculo.kilometraje.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kilometraje") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ingrese el kilometraje actual", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = nuevoKm,
                onValueChange = { nuevoKm = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Kilómetros") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    nuevoKm.toIntOrNull()?.let {
                        viewModel.actualizarKm(it)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Guardar Cambios", fontSize = 18.sp)
            }
        }
    }
}