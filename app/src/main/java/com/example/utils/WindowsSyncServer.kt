package com.example.utils

import android.content.Context
import android.net.wifi.WifiManager
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class WindowsSyncServer(
    private val context: Context,
    private val viewModel: LedgerViewModel
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val port = 8080

    fun start(onStatusChange: (Boolean, String) -> Unit) {
        if (isRunning) return
        isRunning = true
        job = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val ip = getIpAddress()
                withContext(Dispatchers.Main) {
                    onStatusChange(true, "http://$ip:$port")
                }
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        try {
                            handleClient(socket)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isRunning = false
                withContext(Dispatchers.Main) {
                    onStatusChange(false, "")
                }
            }
        }
    }

    fun stop(onStatusChange: (Boolean) -> Unit) {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        job?.cancel()
        onStatusChange(false)
    }

    fun getIpAddress(): String {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wm.connectionInfo.ipAddress
            if (ipAddress != 0) {
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            // ignore wifi manager details and fallback to general interface
        }
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestHeaderLine = reader.readLine() ?: return
            val parts = requestHeaderLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            if (method == "GET" && path == "/") {
                serveDashboard(output)
            } else if (method == "GET" && path == "/api/backup") {
                serveBackupJson(output)
            } else if (method == "GET" && path == "/api/download-launcher") {
                serveDesktopLauncher(output)
            } else if (method == "POST" && path == "/api/restore") {
                handleRestorePost(reader, contentLength, output)
            } else if (method == "GET" && path == "/api/accounts-csv") {
                serveAccountsCsv(output)
            } else {
                sendResponse(output, "HTTP/1.1 404 Not Found", "text/plain", "Page not found".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun sendResponse(output: BufferedOutputStream, status: String, contentType: String, data: ByteArray) {
        val out = PrintWriter(output)
        out.print("$status\r\n")
        out.print("Content-Type: $contentType; charset=utf-8\r\n")
        out.print("Content-Length: ${data.size}\r\n")
        out.print("Connection: close\r\n")
        out.print("\r\n")
        out.flush()
        output.write(data)
        output.flush()
    }

    private fun serveDashboard(output: BufferedOutputStream) {
        val businessName = viewModel.businessName.value
        val businessPhone = viewModel.businessPhone.value
        val businessAddress = viewModel.businessAddress.value
        val defaultCurrency = viewModel.defaultCurrency.value
        
        val accounts = viewModel.accountsWithBalance.value
        val transactionsGrouped = viewModel.allTransactions.value.groupBy { it.accountId }
        
        val accountsJson = JSONArray()
        accounts.forEach { acc ->
            val accObj = JSONObject()
            accObj.put("id", acc.account.id)
            accObj.put("name", acc.account.name)
            accObj.put("phone", acc.account.phone)
            accObj.put("type", acc.account.type)
            accObj.put("tag", acc.account.tag)
            accObj.put("balance", acc.balance)
            accObj.put("transactionCount", acc.transactionCount)
            
            val txArray = JSONArray()
            val txList = transactionsGrouped[acc.account.id] ?: emptyList()
            txList.forEach { tx ->
                val txObj = JSONObject()
                txObj.put("id", tx.id)
                txObj.put("day", tx.day)
                txObj.put("date", tx.date)
                txObj.put("details", tx.details)
                txObj.put("quantity", tx.quantity)
                txObj.put("unitPrice", tx.unitPrice)
                txObj.put("addition", tx.addition)
                txObj.put("total", tx.total)
                txObj.put("isPayment", tx.isPayment)
                txObj.put("timestamp", tx.timestamp)
                txObj.put("currency", tx.currency)
                txArray.put(txObj)
            }
            accObj.put("transactions", txArray)
            accountsJson.put(accObj)
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>الويندوز المدير - تطبيق المحاسب anas لجميع المستخدمين برو</title>
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.rtl.min.css">
                <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@300;400;600;700;800&display=swap" rel="stylesheet">
                <style>
                    body { font-family: 'Cairo', sans-serif; background-color: #f1f5f9; color: #1e293b; }
                    .header-banner { background: linear-gradient(135deg, #1e3a8a, #0f172a); color: white; padding: 25px 20px; border-bottom: 5px solid #d97706; }
                    .card-custom { border: none; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); background-color: white; }
                    .table th { background-color: #334155; color: white !important; font-weight: 700; }
                    .text-success { color: #16a34a !important; }
                    .text-danger { color: #dc2626 !important; }
                    
                    /* Custom Modal Styles */
                    .custom-modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(4px); display: none; align-items: center; justify-content: center; z-index: 2000; padding: 15px; }
                    .custom-modal-card { background: white; border-radius: 16px; width: 100%; max-width: 1000px; max-height: 90vh; display: flex; flex-direction: column; overflow: hidden; border: 1px solid #cbd5e1; }
                    .custom-modal-header { background-color: #1e3a8a; color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #d97706; }
                    .custom-modal-body { padding: 20px; overflow-y: auto; flex-grow: 1; background-color: #f8fafc; }
                    .custom-modal-footer { padding: 12px 20px; border-top: 1px solid #e2e8f0; display: flex; justify-content: flex-end; gap: 10px; background-color: #f1f5f9; }
                    
                    .print-header { display: none; }
                    @media print {
                        body * { visibility: hidden; }
                        #txModal, #txModal * { visibility: visible; }
                        #txModal { display: block !important; position: absolute !important; left: 0; top: 0; width: 100%; background: white !important; }
                        .custom-modal-overlay { position: absolute !important; background: white !important; padding: 0; backdrop-filter: none !important; }
                        .custom-modal-card { box-shadow: none !important; border: none !important; max-height: none !important; width: 100% !important; display: block !important; }
                        .custom-modal-header, .custom-modal-footer, .no-print { display: none !important; }
                        .table-responsive { overflow: visible !important; }
                        .table th, .table td { border: 1px solid #000 !important; color: #000 !important; padding: 5px !important; }
                        .table th { background-color: #f1f5f9 !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                        .print-header { display: block !important; margin-bottom: 20px; border-bottom: 2px double #000; padding-bottom: 15px; }
                    }
                </style>
            </head>
            <body>
                <header class="header-banner no-print">
                    <div class="container d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                        <div>
                            <h1 class="fw-bold mb-1">💻 الويندوز المدير (تطبيق المحاسب anas لجميع المستخدمين برو)</h1>
                            <p class="mb-0 opacity-75 small">إدارة مركزية، مزامنة أوفلاين، ومراقبة أجهزة العمل والمندوبين بدون إنترنت</p>
                        </div>
                        <div class="d-flex flex-wrap gap-2">
                            <span class="badge bg-success py-2 px-3">✓ الويندوز المدير نشط أوفلاين</span>
                            <span class="badge bg-secondary py-2 px-3">المنشأة: $businessName</span>
                        </div>
                    </div>
                </header>

                <main class="container my-4 no-print">
                    <!-- DOWNLOAD DESKTOP APP PROMO BANNER -->
                    <div class="alert card-custom p-3 border-0 mb-4" style="background: linear-gradient(135deg, #1e3a8a, #1d4ed8); color: white; border-right: 5px solid #d97706 !important;">
                        <div class="row align-items-center g-2">
                            <div class="col-md-9">
                                <h5 class="fw-bold mb-1">💻 مشغل الويندوز الرسمي لسطح المكتب (المحافظ)</h5>
                                <p class="mb-0 opacity-90 small">قم بتنزيل أداة التشغيل لسطح المكتب لتشغيل واجهة "الويندوز المدير" كنافذة تطبيق مستقلة وسريعة على ديسكتوب الكمبيوتر الخاص بك مباشرة دون متصفح.</p>
                            </div>
                            <div class="col-md-3 text-md-end">
                                <a href="/api/download-launcher" class="btn btn-warning fw-bold px-3 py-2 btn-sm" style="background-color: #d97706; color: white; border: none; border-radius: 8px;">
                                    📥 تحميل تطبيق سطح المكتب (AnasPro)
                                </a>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Stats row -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <div class="card p-3 card-custom text-center">
                                <h6 class="text-uppercase small text-muted mb-1">عدد الحسابات</h6>
                                <h4 class="fw-bold mb-0 text-primary" id="statAccounts">0</h4>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card p-3 card-custom text-center">
                                <h6 class="text-uppercase small text-muted mb-1">ديون العملاء (لنا)</h6>
                                <h4 class="fw-bold mb-0 text-success" id="statDebtors">YER 0.00</h4>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card p-3 card-custom text-center">
                                <h6 class="text-uppercase small text-muted mb-1">مستحقات الموردين (علينا)</h6>
                                <h4 class="fw-bold mb-0 text-danger" id="statCreditors">YER 0.00</h4>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card p-3 card-custom text-center">
                                <h6 class="text-uppercase small text-muted mb-1">إجمالي الحركات</h6>
                                <h4 class="fw-bold mb-0 text-secondary" id="statOperations">0</h4>
                            </div>
                        </div>
                    </div>

                    <div class="row g-4">
                        <!-- Left Panel: Actions & Backups -->
                        <div class="col-lg-4">
                            <div class="card p-3 card-custom mb-3">
                                <h5 class="fw-bold text-primary mb-2">📦 تصدير قاعدة البيانات</h5>
                                <p class="text-muted small">احفظ نسخة مادية كاملة من قاعدة البيانات على جهاز الكمبيوتر الخاص بك لتأمينها.</p>
                                <div class="btn-group-vertical w-100 gap-2">
                                    <a href="/api/backup" download="AnasPro_Backup_${System.currentTimeMillis() / 1000}.json" class="btn btn-primary text-decoration-none py-2 text-center">
                                        📥 حفظ نسخة احتياطية (.Json)
                                    </a>
                                    <a href="/api/accounts-csv" class="btn btn-outline-secondary py-2 text-center">
                                        📊 تصدير كشوفات الحساب كـ CSV
                                    </a>
                                </div>
                            </div>

                            <div class="card p-3 card-custom">
                                <h5 class="fw-bold text-warning mb-2">📤 استيراد ومزامنة قاعدة البيانات</h5>
                                <p class="text-muted small">قم بتحميل نسخة احتياطية مخزنة مسبقاً (.json) لتعديل ومزامنة كافة الحسابات والعمليات.</p>
                                <form action="/api/restore" method="POST" enctype="multipart/form-data">
                                    <input class="form-control form-control-sm mb-2" type="file" name="backupFile" accept=".json" required>
                                    <button type="submit" class="btn btn-warning w-100 btn-sm text-white fw-bold" onclick="return confirm('تنبيه: سيؤدي هذا الإجراء إلى مسح كافة البيانات الحالية بالكامل وإحلال البيانات المرفوعة، هل تريد الاستمرار؟')">
                                        ⚡ تطبيق المزامنة فوراً
                                    </button>
                                </form>
                            </div>
                        </div>

                        <!-- Right Panel: Data Explorer -->
                        <div class="col-lg-8">
                            <div class="card p-3 card-custom">
                                <div class="d-flex flex-column gap-2 mb-3">
                                    <h5 class="fw-bold text-primary mb-1">📁 سجل الحسابات وإدارة العملاء والموردين</h5>
                                    <div class="row g-2">
                                        <div class="col-md-6 d-flex gap-1 align-items-center">
                                            <span class="small text-muted">التصنيف:</span>
                                            <button class="btn btn-sm btn-outline-primary px-2 py-0 active" id="tabAll" onclick="setCategoryFilter('all')">الكل</button>
                                            <button class="btn btn-sm btn-outline-primary px-2 py-0" id="tabCust" onclick="setCategoryFilter('عميل')">العملاء</button>
                                            <button class="btn btn-sm btn-outline-primary px-2 py-0" id="tabSupp" onclick="setCategoryFilter('مورد')">الموردين</button>
                                        </div>
                                        <div class="col-md-6">
                                            <select class="form-select form-select-sm" id="balFilter" onchange="setStatusFilter(this.value)">
                                                <option value="all">كل الحالات المالية</option>
                                                <option value="debtor">عليهم ديون (مدين)</option>
                                                <option value="creditor">لهم مبالغ (دائن)</option>
                                                <option value="settled">حسابات مخلصة (صفر)</option>
                                            </select>
                                        </div>
                                    </div>
                                    <input type="text" id="searchInput" class="form-control form-control-sm" placeholder="ابحث باسم الحساب، الملاحظات، أو الهاتف..." oninput="handleSearch(this.value)">
                                </div>

                                <div class="table-responsive" style="max-height: 400px; overflow-y: auto;">
                                    <table class="table table-hover table-bordered align-middle text-right small">
                                        <thead>
                                            <tr>
                                                <th>الاسم</th>
                                                <th>الهاتف</th>
                                                <th>التصنيف</th>
                                                <th>المجموعة</th>
                                                <th>الرصيد الحالي</th>
                                                <th>الوضعية المالية</th>
                                                <th>العمليات</th>
                                            </tr>
                                        </thead>
                                        <tbody id="accountsTableBody"></tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>

                <!-- CUSTOM DETAILED STATEMENT MODAL -->
                <div id="txModal" class="custom-modal-overlay">
                    <div class="custom-modal-card">
                        <!-- Print header -->
                        <div class="print-header p-3 text-center">
                            <div class="row align-items-center mb-2">
                                <div class="col-4 text-start">
                                    <h5 class="fw-bold text-dark m-0" id="printBusName">...</h5>
                                    <div class="small text-muted" id="printBusPhone">...</div>
                                    <div class="small text-muted" id="printBusAddress">...</div>
                                </div>
                                <div class="col-4 text-center">
                                    <h4 class="fw-bold text-primary m-0">كشف حساب تفصيلي رسمي</h4>
                                    <span class="badge bg-dark mt-1">نسخة الويندوز المدير (أوفلاين)</span>
                                </div>
                                <div class="col-4 text-end">
                                    <div class="small fw-bold">الاسم: <span id="printAccName">...</span></div>
                                    <div class="small">هاتف: <span id="printAccPhone">...</span></div>
                                    <div class="small">نوع الحساب: <span id="printAccType">...</span></div>
                                    <div class="small text-muted">تاريخ التصدير: <span id="printDate">...</span></div>
                                </div>
                            </div>
                            <div class="row border p-2 mt-2 g-2 text-center" style="background-color: #f8fafc;">
                                <div class="col-4">إجمالي القيود: <strong id="printTotalCharges">YER 0.00</strong></div>
                                <div class="col-4">إجمالي المقبوضات: <strong id="printTotalPayments">YER 0.00</strong></div>
                                <div class="col-4 text-primary">الرصيد النهائي الحالي: <strong id="printNetBalance">YER 0.00</strong></div>
                            </div>
                        </div>

                        <!-- Screen Header -->
                        <div class="custom-modal-header no-print">
                            <h5 class="fw-bold m-0">📂 كشف حساب القيود والحركات والعمليات</h5>
                            <button class="btn close-btn p-0 text-white fs-4" onclick="closeTxModal()">&times;</button>
                        </div>
                        
                        <!-- Screen Body -->
                        <div class="custom-modal-body">
                            <!-- Client Info Summary Block -->
                            <div class="card p-3 mb-3 card-custom border no-print">
                                <div class="row g-2 align-items-center">
                                    <div class="col-md-4">
                                        <h5 class="fw-bold text-primary m-0" id="modalAccName">...</h5>
                                        <div class="text-secondary small">الهاتف: <span id="modalAccPhone">...</span></div>
                                    </div>
                                    <div class="col-md-2 text-center">
                                        <span id="modalAccTypeBadge" class="badge bg-primary">عميل</span>
                                    </div>
                                    <div class="col-md-6 border-start ps-3">
                                        <div class="row text-center">
                                            <div class="col-4">
                                                <div class="small text-muted">إجمالي الفواتير</div>
                                                <strong class="text-dark small" id="modalTotalCharges">0.00</strong>
                                            </div>
                                            <div class="col-4">
                                                <div class="small text-muted">إجمالي المسدد</div>
                                                <strong class="text-success small" id="modalTotalPayments">0.00</strong>
                                            </div>
                                            <div class="col-4">
                                                <div class="p-1 rounded bg-light text-primary" id="modalBalanceContainer">
                                                    <div class="small opacity-80" id="modalBalanceLabel">صافي المتبقي</div>
                                                    <strong class="small" id="modalBalanceVal">0.00</strong>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Search and range filtering toolbar inside the dialog -->
                            <div class="p-2 bg-white rounded border mb-3 no-print">
                                <div class="row g-2">
                                    <div class="col-md-4">
                                        <input type="text" id="txSearch" class="form-control form-control-sm" placeholder="بحث تفصيلي في البيان..." oninput="renderModalTransactions()">
                                    </div>
                                    <div class="col-md-2">
                                        <select class="form-select form-select-sm" id="txTypeFilter" onchange="renderModalTransactions()">
                                            <option value="all">كل الحركات</option>
                                            <option value="charges">القيود والفواتير</option>
                                            <option value="payments">الواصل والمدفوعات</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <input type="date" id="txDateFrom" class="form-control form-control-sm" onchange="renderModalTransactions()">
                                    </div>
                                    <div class="col-md-3">
                                        <input type="date" id="txDateTo" class="form-control form-control-sm" onchange="renderModalTransactions()">
                                    </div>
                                </div>
                            </div>

                            <!-- Table view -->
                            <div class="table-responsive">
                                <table class="table table-bordered text-right small">
                                    <thead>
                                        <tr class="table-dark">
                                            <th>التاريخ واليوم</th>
                                            <th>التفاصيل والبيان</th>
                                            <th>الكمية والسعر</th>
                                            <th>الإضافي</th>
                                            <th>الإجمالي</th>
                                            <th>نوع الحركة</th>
                                        </tr>
                                    </thead>
                                    <tbody id="modalTransactionsTableBody"></tbody>
                                </table>
                            </div>
                        </div>
                        
                        <!-- Screen Footer Actions -->
                        <div class="custom-modal-footer no-print">
                            <button class="btn btn-warning btn-sm fw-bold px-3 py-2 text-white" onclick="window.print()">
                                🖨️ طباعة كشف حساب وتصدير PDF رسمي أوفلاين
                            </button>
                            <button class="btn btn-secondary btn-sm px-3 py-2" onclick="closeTxModal()">
                                إغلاق
                            </button>
                        </div>
                    </div>
                </div>

                <footer class="text-center py-3 text-muted mt-5 bg-white border-top no-print">
                    <p class="mb-0 small">تطبيق المحاسب anas لجميع المستخدمين برو © 2026 - نظام سطح المكتب المدير العام الأوفلاين</p>
                </footer>

                <!-- Inject logic -->
                <script>
                    const accountsData = ${accountsJson.toString()};
                    const defaultCurrency = "$defaultCurrency";
                    const businessName = "${businessName.replace("\"", "\\\"")}";
                    const businessPhone = "${businessPhone.replace("\"", "\\\"")}";
                    const businessAddress = "${businessAddress.replace("\"", "\\\"")}";
                    
                    let selectedCategory = 'all'; 
                    let selectedStatus = 'all';
                    let searchQueryGlobal = '';
                    let currentAccountId = null;
                    
                    window.onload = function() {
                        updateGeneralStats();
                        renderAccounts();
                    };
                    
                    function updateGeneralStats() {
                        let totalAccounts = accountsData.length;
                        let totalDebtorsSum = 0;
                        let totalCreditorsSum = 0;
                        let totalTxCount = 0;
                        
                        accountsData.forEach(acc => {
                            totalTxCount += acc.transactionCount;
                            let b = acc.balance;
                            if (acc.type === "مورد") {
                                if (b >= 0) totalCreditorsSum += b;
                                else totalDebtorsSum += Math.abs(b);
                            } else {
                                if (b >= 0) totalDebtorsSum += b;
                                else totalCreditorsSum += Math.abs(b);
                            }
                        });
                        
                        document.getElementById('statAccounts').innerText = totalAccounts;
                        document.getElementById('statDebtors').innerText = formatCurrency(totalDebtorsSum);
                        document.getElementById('statCreditors').innerText = formatCurrency(totalCreditorsSum);
                        document.getElementById('statOperations').innerText = totalTxCount;
                    }
                    
                    function formatCurrency(num) {
                        return Number(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " " + defaultCurrency;
                    }
                    
                    function setCategoryFilter(category) {
                        selectedCategory = category;
                        document.getElementById('tabAll').classList.remove('active');
                        document.getElementById('tabCust').classList.remove('active');
                        document.getElementById('tabSupp').classList.remove('active');
                        
                        if (category === 'all') document.getElementById('tabAll').classList.add('active');
                        else if (category === 'عميل') document.getElementById('tabCust').classList.add('active');
                        else if (category === 'مورد') document.getElementById('tabSupp').classList.add('active');
                        
                        renderAccounts();
                    }
                    
                    function setStatusFilter(status) {
                        selectedStatus = status;
                        renderAccounts();
                    }
                    
                    function handleSearch(val) {
                        searchQueryGlobal = val.trim().toLowerCase();
                        renderAccounts();
                    }
                    
                    function renderAccounts() {
                        const tbody = document.getElementById('accountsTableBody');
                        tbody.innerHTML = '';
                        
                        let filtered = accountsData.filter(acc => {
                            if (selectedCategory !== 'all' && acc.type !== selectedCategory) return false;
                            
                            const isSupplier = acc.type === 'مورد';
                            const b = acc.balance;
                            
                            if (selectedStatus === 'debtor') {
                                if (isSupplier && b >= 0) return false;
                                if (!isSupplier && b <= 0) return false;
                            } else if (selectedStatus === 'creditor') {
                                if (isSupplier && b <= 0) return false;
                                if (!isSupplier && b >= 0) return false;
                            } else if (selectedStatus === 'settled') {
                                if (b !== 0.0) return false;
                            }
                            
                            if (searchQueryGlobal) {
                                const tag = acc.tag ? acc.tag.toLowerCase() : '';
                                const phone = acc.phone ? acc.phone.toLowerCase() : '';
                                const name = acc.name.toLowerCase();
                                if (!name.includes(searchQueryGlobal) && !phone.includes(searchQueryGlobal) && !tag.includes(searchQueryGlobal)) {
                                    return false;
                                }
                            }
                            return true;
                        });
                        
                        if (filtered.length === 0) {
                            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted p-3">لا توجد حسابات مسجلة مطابقة.</td></tr>`;
                            return;
                        }
                        
                        filtered.forEach(acc => {
                            const isSupplier = acc.type === 'مورد';
                            const b = acc.balance;
                            let directionLabel = '';
                            let balanceColorClass = '';
                            
                            if (b === 0.0) {
                                directionLabel = "مخلص";
                                balanceColorClass = "text-muted";
                            } else if (isSupplier) {
                                if (b >= 0) { directionLabel = "لهم علينا (دائن)"; balanceColorClass = "text-danger"; }
                                else { directionLabel = "لنا عليهم (مدين)"; balanceColorClass = "text-success"; }
                            } else {
                                if (b >= 0) { directionLabel = "عليهم لنا (مدين)"; balanceColorClass = "text-danger"; }
                                else { directionLabel = "لهم علينا (دائن)"; balanceColorClass = "text-success"; }
                            }
                            
                            tbody.innerHTML += `
                                <tr>
                                    <td><strong>${'$'}{acc.name}</strong></td>
                                    <td>${'$'}{acc.phone ? acc.phone : '<span class="text-muted">غير متوفر</span>'}</td>
                                    <td><span class="badge ${'$'}{isSupplier ? 'bg-warning text-dark' : 'bg-info text-dark'}">${'$'}{acc.type}</span></td>
                                    <td class="text-secondary small">${'$'}{acc.tag ? acc.tag : '-'}</td>
                                    <td><strong>${'$'}{formatCurrency(Math.abs(b))}</strong></td>
                                    <td><span class="fw-bold ${'$'}{balanceColorClass}">${'$'}{directionLabel}</span></td>
                                    <td>
                                        <button class="btn btn-primary btn-sm px-2 py-0 fw-bold" onclick="openTxModal(${'$'}{acc.id})">
                                            👁️ كشف الحساب
                                        </button>
                                    </td>
                                </tr>
                            `;
                        });
                    }
                    
                    function openTxModal(id) {
                        const acc = accountsData.find(a => a.id === id);
                        if (!acc) return;
                        
                        currentAccountId = id;
                        document.getElementById('modalAccName').innerText = acc.name;
                        document.getElementById('modalAccPhone').innerText = acc.phone ? acc.phone : "غير متوفر";
                        document.getElementById('modalAccTypeBadge').innerText = acc.type;
                        document.getElementById('modalAccTypeBadge').className = acc.type === 'مورد' ? 'badge bg-warning text-dark' : 'badge bg-info text-dark';
                        
                        const txList = acc.transactions || [];
                        let charges = 0, payments = 0;
                        txList.forEach(tx => {
                            if (tx.isPayment) payments += tx.total;
                            else charges += tx.total;
                        });
                        
                        const balance = charges - payments;
                        document.getElementById('modalTotalCharges').innerText = formatCurrency(charges);
                        document.getElementById('modalTotalPayments').innerText = formatCurrency(payments);
                        document.getElementById('modalBalanceVal').innerText = formatCurrency(Math.abs(balance));
                        
                        const balLabel = document.getElementById('modalBalanceLabel');
                        const balContainer = document.getElementById('modalBalanceContainer');
                        
                        if (balance === 0) {
                            balLabel.innerText = "الحساب مخلص ومصفر";
                            balContainer.className = "p-1 rounded bg-light text-secondary";
                        } else if (acc.type === 'مورد') {
                            if (balance > 0) { balLabel.innerText = "لهم علينا (دائن)"; balContainer.className = "p-1 rounded bg-danger-subtle text-danger"; }
                            else { balLabel.innerText = "تم دفع زيادة (مدين)"; balContainer.className = "p-1 rounded bg-success-subtle text-success"; }
                        } else {
                            if (balance > 0) { balLabel.innerText = "عليهم لنا (مدين)"; balContainer.className = "p-1 rounded bg-danger-subtle text-danger"; }
                            else { balLabel.innerText = "الرصيد المسدد زيادة (له)"; balContainer.className = "p-1 rounded bg-success-subtle text-success"; }
                        }
                        
                        document.getElementById('printBusName').innerText = businessName;
                        document.getElementById('printBusPhone').innerText = businessPhone ? 'هاتف: ' + businessPhone : '';
                        document.getElementById('printBusAddress').innerText = businessAddress ? 'العنوان: ' + businessAddress : '';
                        document.getElementById('printAccName').innerText = acc.name;
                        document.getElementById('printAccPhone').innerText = acc.phone ? acc.phone : '';
                        document.getElementById('printAccType').innerText = acc.type;
                        document.getElementById('printDate').innerText = new Date().toLocaleString('ar-AE');
                        document.getElementById('printTotalCharges').innerText = formatCurrency(charges);
                        document.getElementById('printTotalPayments').innerText = formatCurrency(payments);
                        document.getElementById('printNetBalance').innerText = formatCurrency(Math.abs(balance)) + " " + (balance >= 0 ? (acc.type === 'مورد' ? '(دائن)' : '(مدين)') : (acc.type === 'مورد' ? '(مدين)' : '(دائن)'));

                        document.getElementById('txSearch').value = '';
                        document.getElementById('txTypeFilter').value = 'all';
                        document.getElementById('txDateFrom').value = '';
                        document.getElementById('txDateTo').value = '';
                        
                        renderModalTransactions();
                        document.getElementById('txModal').style.display = 'flex';
                    }
                    
                    function closeTxModal() {
                        document.getElementById('txModal').style.display = 'none';
                        currentAccountId = null;
                    }
                    
                    function renderModalTransactions() {
                        const acc = accountsData.find(a => a.id === currentAccountId);
                        if (!acc) return;
                        
                        const searchQuery = document.getElementById('txSearch').value.trim().toLowerCase();
                        const typeFilter = document.getElementById('txTypeFilter').value;
                        const dateFrom = document.getElementById('txDateFrom').value;
                        const dateTo = document.getElementById('txDateTo').value;
                        
                        let filtered = (acc.transactions || []).filter(tx => {
                            if (searchQuery && !tx.details.toLowerCase().includes(searchQuery)) return false;
                            if (typeFilter === 'charges' && tx.isPayment) return false;
                            if (typeFilter === 'payments' && !tx.isPayment) return false;
                            if (dateFrom && tx.date < dateFrom) return false;
                            if (dateTo && tx.date > dateTo) return false;
                            return true;
                        });
                        
                        filtered.sort((a,b) => b.timestamp - a.timestamp);
                        const tbody = document.getElementById('modalTransactionsTableBody');
                        tbody.innerHTML = '';
                        
                        if (filtered.length === 0) {
                            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">لا توجد حركات مالية مطابقة.</td></tr>`;
                            return;
                        }
                        
                        filtered.forEach(tx => {
                            const badge = tx.isPayment 
                                ? `<span class="badge bg-success-subtle text-success border border-success px-2 py-0">🟢 دفعة/واصل</span>`
                                : `<span class="badge bg-danger-subtle text-danger border border-danger px-2 py-0">🔴 فاتورة/قيد</span>`;
                                
                            tbody.innerHTML += `
                                <tr>
                                    <td><strong>${'$'}{tx.date}</strong> <span class="text-muted small">(${'$'}{tx.day})</span></td>
                                    <td>${'$'}{tx.details}</td>
                                    <td class="text-secondary">${'$'}{tx.isPayment ? '-' : tx.quantity + ' × ' + formatCurrency(tx.unitPrice)}</td>
                                    <td class="text-secondary">${'$'}{tx.isPayment ? '-' : formatCurrency(tx.addition)}</td>
                                    <td><strong>${'$'}{formatCurrency(tx.total)}</strong></td>
                                    <td>${'$'}{badge}</td>
                                </tr>
                            `;
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        sendResponse(output, "HTTP/1.1 200 OK", "text/html", htmlContent.toByteArray(Charsets.UTF_8))
    }

    private fun serveBackupJson(output: BufferedOutputStream) {
        viewModel.exportDatabaseJson { file ->
            if (file != null) {
                try {
                    val bytes = file.readBytes()
                    sendResponse(output, "HTTP/1.1 200 OK", "application/json", bytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    sendResponse(output, "HTTP/1.1 500 Internal Error", "text/plain", "Failed to read backup".toByteArray())
                }
            } else {
                sendResponse(output, "HTTP/1.1 500 Internal Error", "text/plain", "Database export returned null".toByteArray())
            }
        }
    }

    private fun handleRestorePost(reader: BufferedReader, contentLength: Int, output: BufferedOutputStream) {
        try {
            if (contentLength <= 0) {
                sendResponse(output, "HTTP/1.1 400 Bad Request", "text/html", "<h3>عذراً، يجب إرفاق ملف لرفع نسخة البيانات بالكامل!</h3>".toByteArray(Charsets.UTF_8))
                return
            }

            // Read request body contents
            val buffer = CharArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = reader.read(buffer, totalRead, contentLength - totalRead)
                if (read == -1) break
                totalRead += read
            }

            val rawBody = String(buffer)
            
            // Extract boundaries of file inside multipart/form-data or pure body
            var cleanJsonStringString = ""
            if (rawBody.contains("{") && rawBody.contains("}")) {
                val startIndex = rawBody.indexOf("{")
                val endIndex = rawBody.lastIndexOf("}")
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    cleanJsonStringString = rawBody.substring(startIndex, endIndex + 1)
                }
            }

            if (cleanJsonStringString.isBlank()) {
                sendResponse(output, "HTTP/1.1 400 Bad Request", "text/html", "<h3>لم نتمكن من العثور على محتوى قاعدة بيانات صالح، يرجى رفع ملف JSON صحيح وصادر عن التطبيق.</h3>".toByteArray(Charsets.UTF_8))
                return
            }

            // Let viewmodel complete the parse structure and launch import database
            viewModel.restoreDatabaseJson(cleanJsonStringString) { isSuccess ->
                if (isSuccess) {
                    val htmlSuccess = """
                        <!DOCTYPE html>
                        <html lang="ar" dir="rtl">
                        <head>
                            <meta charset="UTF-8">
                            <title>مزامنة ناجحة</title>
                            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.rtl.min.css">
                            <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@600;800&display=swap" rel="stylesheet">
                            <style>body { font-family: 'Cairo', sans-serif; background-color: #f8fafc; text-align: center; padding-top: 100px; }</style>
                        </head>
                        <body>
                            <div class="container card p-5 shadow-lg max-width: 500px;">
                                <h1 class="text-success fw-bold">🚀 تم استيراد ومزامنة كافة البيانات بالكامل!</h1>
                                <p class="text-secondary mt-3">تم تحديث تطبيق الحسابات "anas برو" على الهاتف ومزامنته بملف الويندوز بنجاح.</p>
                                <a href="/" class="btn btn-primary mt-4">✓ العودة للوحة الإدارة الرئيسية</a>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    sendResponse(output, "HTTP/1.1 200 OK", "text/html", htmlSuccess.toByteArray(Charsets.UTF_8))
                } else {
                    sendResponse(output, "HTTP/1.1 500 Internal Server Error", "text/html", "<h3>فشل إحلال ومزامنة البيانات، يرجى التأكد من ملائمة ملف الـ JSON ومطابقته لبنية الحسابات الخاصة بنا.</h3>".toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(output, "HTTP/1.1 500 Internal Error", "text/plain", e.localizedMessage?.toByteArray() ?: "Error".toByteArray())
        }
    }

    private fun serveAccountsCsv(output: BufferedOutputStream) {
        val accounts = viewModel.accountsWithBalance.value
        val csv = StringBuilder()
        
        // CSV header
        csv.append("الاسم,الهاتف,التصنيف,الوسم,الرصيد الحالي,حالة رصيد الحساب,عدد الحركات\n")
        
        accounts.forEach { acc ->
            val b = acc.balance
            val isCreditor = acc.account.type == "مورد"
            val stateText = if (b == 0.0) {
                "مخلص"
            } else if (isCreditor) {
                if (b >= 0) "لهم علينا (دائن)" else "لنا عليهم (مدين)"
            } else {
                if (b >= 0) "عليهم لنا (مدين)" else "لهم علينا (دائن)"
            }

            val sanitizedName = acc.account.name.replace(",", " ")
            val sanitizedTag = acc.account.tag.replace(",", " ")
            val formattedBal = String.format(Locale.US, "%.2f", Math.abs(b))
            
            csv.append("$sanitizedName,${acc.account.phone},$isCreditor,$sanitizedTag,$formattedBal,$stateText,${acc.transactionCount}\n")
        }

        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csvBytes = csv.toString().toByteArray(Charsets.UTF_8)
        
        val fullData = ByteArray(bom.size + csvBytes.size)
        System.arraycopy(bom, 0, fullData, 0, bom.size)
        System.arraycopy(csvBytes, 0, fullData, bom.size, csvBytes.size)

        var respPrintWriter = PrintWriter(output)
        respPrintWriter.print("HTTP/1.1 200 OK\r\n")
        respPrintWriter.print("Content-Type: text/csv; charset=utf-8\r\n")
        respPrintWriter.print("Content-Disposition: attachment; filename=\"AnasPro_Accounts_Sync_${System.currentTimeMillis() / 1000}.csv\"\r\n")
        respPrintWriter.print("Content-Length: ${fullData.size}\r\n")
        respPrintWriter.print("Connection: close\r\n")
        respPrintWriter.print("\r\n")
        respPrintWriter.flush()
        
        output.write(fullData)
        output.flush()
    }

    private fun serveDesktopLauncher(output: BufferedOutputStream) {
        val phoneIp = getIpAddress()
        val batScript = """
            @echo off
            chcp 65001 >nul
            title تطبيق المحاسب anas لجميع المستخدمين برو - مشغل سطح المكتب المسؤول
            color 0B
            cls
            echo =======================================================================
            echo          تطبيق المحاسب anas لجميع المستخدمين برو (نسخة سطح المكتب والمدير)
            echo =======================================================================
            echo  [+] جاري تشغيل المشغل الرسمي لـ تطبيق المحاسب anas لجميع المستخدمين برو...
            echo  [+] عنوان الخادم المقترن: http://$phoneIp:$port
            echo =======================================================================
            echo.
            echo  يرجى التأكد من تشغيل خادم المزامنة في إعدادات التطبيق على الهاتف أولا.
            echo  وتأكد من اتصال الكمبيوتر والهاتف بـ (نفس شبكة الواي فاي Wi-Fi).
            echo.
            echo  جاري تشغيل واجهة الإدارة والتحكم الرسمية بنمط تطبيق مثبت ومستقل...
            echo.
            
            :: Launch MS Edge in standalone chromeless application mode
            start msedge --app=http://$phoneIp:$port/
            if %ERRORLEVEL% NEQ 0 (
                :: Fallback to default browser
                start http://$phoneIp:$port/
            )
            
            echo  [✓] تم التشغيل بنجاح! يمكنك الآن إغلاق هذه النافذة أو تصغيرها.
            timeout /t 5 >nul
            exit
        """.trimIndent()

        val data = batScript.toByteArray(Charsets.UTF_8)
        
        var respPrintWriter = PrintWriter(output)
        respPrintWriter.print("HTTP/1.1 200 OK\r\n")
        respPrintWriter.print("Content-Type: application/octet-stream\r\n")
        respPrintWriter.print("Content-Disposition: attachment; filename=\"AnasPro_Desktop_Manager.bat\"\r\n")
        respPrintWriter.print("Content-Length: ${data.size}\r\n")
        respPrintWriter.print("Connection: close\r\n")
        respPrintWriter.print("\r\n")
        respPrintWriter.flush()
        
        output.write(data)
        output.flush()
    }
}
