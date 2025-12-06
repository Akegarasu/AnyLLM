package com.anzu.anyllm.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anzu.anyllm.R
import com.anzu.anyllm.model.Profile
import com.anzu.anyllm.viewmodel.ProfileViewModel
import com.anzu.anyllm.viewmodel.TestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: String?,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    // 保持编辑时使用的 ID（确保编辑不会丢失关联的会话）
    val editingProfileId = remember(profileId) { profileId }

    // 表单状态
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("POST") }
    val headers = remember { mutableStateListOf<Pair<String, String>>() }
    var bodyTemplate by remember { mutableStateOf("") }
    var responseJsonPath by remember { mutableStateOf("") }
    var streamEnabled by remember { mutableStateOf(false) }
    var streamJsonPath by remember { mutableStateOf("") }
    var timeoutSeconds by remember { mutableStateOf("60") }
    var apiKey by remember { mutableStateOf("") }

    // 加载现有配置
    LaunchedEffect(profileId) {
        if (profileId != null) {
            viewModel.loadProfile(profileId)
        }
    }

    LaunchedEffect(selectedProfile) {
        selectedProfile?.let { profile ->
            name = profile.name
            baseUrl = profile.baseUrl
            method = profile.method
            headers.clear()
            headers.addAll(profile.headers.toList())
            bodyTemplate = profile.bodyTemplate
            responseJsonPath = profile.responseJsonPath
            streamEnabled = profile.streamEnabled
            streamJsonPath = profile.streamJsonPath ?: ""
            timeoutSeconds = profile.timeoutSeconds.toString()
            apiKey = viewModel.getApiKey(profile.id) ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (profileId == null) stringResource(R.string.profile_create)
                        else stringResource(R.string.profile_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedProfile()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val profile = createProfile(
                                existingId = editingProfileId,  // 使用传入的 profileId 保持关联
                                name = name,
                                baseUrl = baseUrl,
                                method = method,
                                headers = headers.toMap(),
                                bodyTemplate = bodyTemplate,
                                responseJsonPath = responseJsonPath,
                                streamEnabled = streamEnabled,
                                streamJsonPath = streamJsonPath.ifBlank { null },
                                timeoutSeconds = timeoutSeconds.toIntOrNull() ?: 60
                            )
                            if (apiKey.isNotBlank()) {
                                viewModel.saveApiKey(profile.id, apiKey)
                            }
                            viewModel.saveProfile(profile)
                            viewModel.clearSelectedProfile()
                            onSaveComplete()
                        },
                        enabled = name.isNotBlank() && baseUrl.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 基本信息
            SectionTitle(stringResource(R.string.profile_section_basic))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.profile_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // HTTP 方法选择
            HttpMethodSelector(
                selectedMethod = method,
                onMethodSelected = { method = it }
            )

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.profile_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            // 请求头
            SectionTitle(stringResource(R.string.profile_section_headers))

            headers.forEachIndexed { index, (key, value) ->
                HeaderRow(
                    headerKey = key,
                    headerValue = value,
                    onKeyChange = { headers[index] = it to headers[index].second },
                    onValueChange = { headers[index] = headers[index].first to it },
                    onDelete = { headers.removeAt(index) }
                )
            }

            OutlinedButton(
                onClick = { headers.add("" to "") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_add_header))
            }

            // 请求体模板
            SectionTitle(stringResource(R.string.profile_section_body))

            OutlinedTextField(
                value = bodyTemplate,
                onValueChange = { bodyTemplate = it },
                label = { Text(stringResource(R.string.profile_body_template)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 20
            )

            // 响应提取
            SectionTitle(stringResource(R.string.profile_section_response))

            OutlinedTextField(
                value = responseJsonPath,
                onValueChange = { responseJsonPath = it },
                label = { Text(stringResource(R.string.profile_response_jsonpath)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("$.choices[0].message.content") }
            )

            // 流式输出
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile_stream_enabled))
                Switch(
                    checked = streamEnabled,
                    onCheckedChange = { streamEnabled = it }
                )
            }

            if (streamEnabled) {
                OutlinedTextField(
                    value = streamJsonPath,
                    onValueChange = { streamJsonPath = it },
                    label = { Text(stringResource(R.string.profile_stream_jsonpath)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("$.choices[0].delta.content") }
                )
            }

            // 超时设置
            OutlinedTextField(
                value = timeoutSeconds,
                onValueChange = { timeoutSeconds = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.profile_timeout)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(stringResource(R.string.seconds)) }
            )

            // 测试按钮
            Button(
                onClick = {
                    val profile = createProfile(
                        existingId = editingProfileId,  // 使用传入的 profileId
                        name = name,
                        baseUrl = baseUrl,
                        method = method,
                        headers = headers.toMap(),
                        bodyTemplate = bodyTemplate,
                        responseJsonPath = responseJsonPath,
                        streamEnabled = false, // 测试时使用非流式
                        streamJsonPath = null,
                        timeoutSeconds = timeoutSeconds.toIntOrNull() ?: 60
                    )
                    if (apiKey.isNotBlank()) {
                        viewModel.saveApiKey(profile.id, apiKey)
                    }
                    viewModel.testProfile(profile, "Hello")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting && baseUrl.isNotBlank()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_test))
            }

            // 测试结果
            testResult?.let { result ->
                TestResultCard(result = result)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HttpMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val methods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedMethod,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.profile_method)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            methods.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method) },
                    onClick = {
                        onMethodSelected(method)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    headerKey: String,
    headerValue: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = headerKey,
            onValueChange = onKeyChange,
            label = { Text(stringResource(R.string.profile_header_key)) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = headerValue,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.profile_header_value)) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is TestResult.Success -> MaterialTheme.colorScheme.primaryContainer
                is TestResult.Error -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (result) {
                    is TestResult.Success -> stringResource(R.string.test_success)
                    is TestResult.Error -> stringResource(R.string.test_failed)
                },
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (result) {
                    is TestResult.Success -> result.content
                    is TestResult.Error -> result.message
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 10
            )
        }
    }
}

private fun createProfile(
    existingId: String?,
    name: String,
    baseUrl: String,
    method: String,
    headers: Map<String, String>,
    bodyTemplate: String,
    responseJsonPath: String,
    streamEnabled: Boolean,
    streamJsonPath: String?,
    timeoutSeconds: Int
): Profile {
    return Profile(
        id = existingId ?: java.util.UUID.randomUUID().toString(),
        name = name,
        baseUrl = baseUrl,
        method = method,
        headers = headers,
        bodyTemplate = bodyTemplate,
        responseJsonPath = responseJsonPath,
        streamEnabled = streamEnabled,
        streamJsonPath = streamJsonPath,
        timeoutSeconds = timeoutSeconds
    )
}

private fun List<Pair<String, String>>.toMap(): Map<String, String> {
    return filter { it.first.isNotBlank() }.associate { it }
}

