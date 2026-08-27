# -*- coding: utf-8 -*-
"""
================================================================================
تطبيق إدارة وتحكم السيرفر السحابي لويندوز 💻 - المحاسب anas برو (Admin Dashboard)
================================================================================
هذا الكود مبني باستخدام مكتبة Flet الحديثة في بايثون لتقديم واجهة مستخدم متميزة (Material Design 3).
يتكامل التطبيق مع خادم سحابي لقاعدة بيانات (أو ملف محلي كقاعدة بيانات محاكاة) للتحكم في العملاء المشتركين بالتطبيق.

💡 متطلبات التشغيل على جهاز الويندوز:
1. تأكد من تثبيت Python (نسخة 3.8 أو أحدث) من الموقع الرسمي python.org
2. افتح منفذ الأوامر (CMD) وثبت المكتبة عبر الأمر:
   pip install flet requests

3. لتشغيل التطبيق، اكتب في منفذ الأوامر:
   flet run AnasPro_Windows_Admin.py
================================================================================
"""

import flet as ft
import json
import os
import time
from datetime import datetime

# اسم ملف قاعدة بيانات المحاكاة لتجربة الحفظ وتأثير زر التجميد والتفعيل محلياً وسحابياً
DB_FILE = "cloud_simulation_db.json"

# بيانات تجريبية افتراضية في حال عدم وجود ملف منشأ مسبقاً
DEFAULT_CLIENTS = [
    {
        "client_id": "anas-pro-client-7711",
        "business_name": "مؤسسة أنس للمقاولات العامة",
        "business_phone": "+967 770123456",
        "accounts_count": 48,
        "transactions_count": 312,
        "backup_date": int(time.time() * 1000) - 250000,
        "is_frozen": False,
        "data_size_kb": 245.8
    },
    {
        "client_id": "anas-pro-client-5322",
        "business_name": "سوبرماركت الأمانة والخير",
        "business_phone": "+967 775555444",
        "accounts_count": 125,
        "transactions_count": 1420,
        "backup_date": int(time.time() * 1000) - 100000,
        "is_frozen": False,
        "data_size_kb": 890.4
    },
    {
        "client_id": "anas-pro-client-9884",
        "business_name": "شركة عدن للاستيراد والتصدير",
        "business_phone": "+967 733999111",
        "accounts_count": 92,
        "transactions_count": 870,
        "backup_date": int(time.time() * 1000) - 1500000,
        "is_frozen": True,
        "data_size_kb": 512.0
    }
]

def load_database():
    """تحميل العملاء من قاعدة البيانات المحلية"""
    if os.path.exists(DB_FILE):
        try:
            with open(DB_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return DEFAULT_CLIENTS
    else:
        save_database(DEFAULT_CLIENTS)
        return DEFAULT_CLIENTS

def save_database(data):
    """حفظ العملاء في قاعدة البيانات"""
    try:
        with open(DB_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)
    except Exception as e:
        print(f"Error saving database: {e}")

def main(page: ft.Page):
    page.title = "المحاسب anas برو - لوحة التحكم الإدارية السحابية للويندوز"
    page.window_width = 1100
    page.window_height = 750
    page.window_min_width = 900
    page.window_min_height = 650
    page.theme_mode = ft.ThemeMode.DARK
    page.rtl = True # تشغيل الواجهة بالكامل باللغة العربية (اليمين لليسار)

    # تحميل البيانات
    clients = load_database()

    # عناصر واجهة المستخدم للبحث
    search_input = ft.TextField(
        label="ابحث باسم المنشأة، معرف العميل أو رقم الهاتف...",
        prefix_icon=ft.icons.SEARCH,
        border_radius=12,
        width=450,
        text_size=13,
        content_padding=15,
    )

    # بطاقات الإحصائيات (KPIs)
    total_users_card = ft.Text("0", size=32, weight=ft.FontWeight.BOLD, color=ft.colors.BLUE_400)
    active_users_card = ft.Text("0", size=32, weight=ft.FontWeight.BOLD, color=ft.colors.GREEN_400)
    frozen_users_card = ft.Text("0", size=32, weight=ft.FontWeight.BOLD, color=ft.colors.RED_400)
    vol_card = ft.Text("0 KB", size=32, weight=ft.FontWeight.BOLD, color=ft.colors.AMBER_400)

    clients_table = ft.DataTable(
        columns=[
            ft.DataColumn(ft.Text("معرف العميل (Client ID)", weight=ft.FontWeight.BOLD)),
            ft.DataColumn(ft.Text("اسم المنشأة والمالك", weight=ft.FontWeight.BOLD)),
            ft.DataColumn(ft.Text("رقم الهاتف", weight=ft.FontWeight.BOLD)),
            ft.DataColumn(ft.Text("عدد الحسابات", weight=ft.FontWeight.BOLD, numeric=True)),
            ft.DataColumn(ft.Text("عدد العمليات", weight=ft.FontWeight.BOLD, numeric=True)),
            ft.DataColumn(ft.Text("حجم البيانات", weight=ft.FontWeight.BOLD, numeric=True)),
            ft.DataColumn(ft.Text("حالة الجهاز", weight=ft.FontWeight.BOLD)),
            ft.DataColumn(ft.Text("الإجراء والتحكم الإداري", weight=ft.FontWeight.BOLD)),
        ],
        rows=[]
    )

    def calculate_stats():
        """حساب وتحديث بطاقات الإحصائيات"""
        total = len(clients)
        active = sum(1 for c in clients if not c.get("is_frozen", False))
        frozen = total - active
        total_size = sum(c.get("data_size_kb", 0.0) for c in clients)
        
        total_users_card.value = str(total)
        active_users_card.value = str(active)
        frozen_users_card.value = str(frozen)
        vol_card.value = f"{total_size:.1f} KB"
        page.update()

    def handle_status_change(client_id, freeze_action):
        """تغيير حالة تفعيل العميل وتحديث الملف"""
        for client in clients:
            if client["client_id"] == client_id:
                client["is_frozen"] = freeze_action
                break
        save_database(clients)
        calculate_stats()
        render_table()
        
        status_word = "تجميد" if freeze_action else "تفعيل"
        color = ft.colors.RED if freeze_action else ft.colors.GREEN
        
        # إشعار سريع أسفل الشاشة
        snack = ft.SnackBar(
            content=ft.Text(f"✓ تم {status_word} حساب العميل ({client_id}) بنجاح!", size=12, text_align=ft.TextAlign.RIGHT),
            bgcolor=color,
            duration=3000
        )
        page.overlay.append(snack)
        snack.open = True
        page.update()

    def handle_add_client(e):
        """إضافة عميل يدويًا لمحاكاة نظام حقيقي"""
        def save_new_client(ev):
            if not name_field.value or not id_field.value:
                return
            
            new_c = {
                "client_id": id_field.value,
                "business_name": name_field.value,
                "business_phone": phone_field.value or "غير متوفر",
                "accounts_count": int(accs_field.value or 0),
                "transactions_count": int(txs_field.value or 0),
                "backup_date": int(time.time() * 1000),
                "is_frozen": False,
                "data_size_kb": float(size_field.value or 2.5)
            }
            clients.append(new_c)
            save_database(clients)
            dialog.open = False
            calculate_stats()
            render_table()
            page.update()

        name_field = ft.TextField(label="اسم المنشأة/العميل جديد", border_radius=10)
        id_field = ft.TextField(label="معرّف العميل (Client ID)", value=f"anas-pro-{int(time.time())%10000}", border_radius=10)
        phone_field = ft.TextField(label="رقم الجوال", border_radius=10)
        accs_field = ft.TextField(label="عدد الحسابات", value="0", border_radius=10)
        txs_field = ft.TextField(label="عدد القيود/العمليات", value="0", border_radius=10)
        size_field = ft.TextField(label="حجم تقريبي للبيانات (KB)", value="10.5", border_radius=10)

        dialog = ft.AlertDialog(
            title=ft.Text("إضافة عميل مشترك جديد سحابياً ➕", text_align=ft.TextAlign.RIGHT),
            content=ft.Column([
                name_field, id_field, phone_field,
                ft.Row([accs_field, txs_field, size_field], alignment=ft.MainAxisAlignment.SPACE_BETWEEN)
            ], height=340, spacing=10),
            actions=[
                ft.Button("إلغاء", on_click=lambda _: setattr(dialog, "open", False)),
                ft.ElevatedButton("إضافة العميل وتفعيله", on_click=save_new_client, bgcolor=ft.colors.GREEN_600, color=ft.colors.WHITE),
            ],
            actions_alignment=ft.MainAxisAlignment.END,
        )
        page.overlay.append(dialog)
        dialog.open = True
        page.update()

    def render_table(query=""):
        """بناء واستعراض كشف الحسابات والعملاء في الجدول"""
        clients_table.rows.clear()
        q = query.lower()

        for c in clients:
            if (q in c["client_id"].lower() or 
                q in c["business_name"].lower() or 
                q in c["business_phone"].lower()):
                
                # تنسيق تاريخ المزامنة
                millis = c.get("backup_date", 0)
                date_str = "لم يزامن بعد"
                if millis > 0:
                    try:
                        date_str = datetime.fromtimestamp(millis / 1000.0).strftime("%Y-%m-%d %I:%M %p")
                    except Exception:
                        pass

                # حالة الجهاز تجميد/تفعيل
                is_frozen = c.get("is_frozen", False)
                status_icon = ft.icons.CHECK_CIRCLE if not is_frozen else ft.icons.BLOCK
                status_color = ft.colors.GREEN_400 if not is_frozen else ft.colors.RED_400
                status_text = "نشط ومفعّل" if not is_frozen else "معلق ومجمد"

                action_buttons = ft.Row([
                    ft.IconButton(
                        icon=ft.icons.SHIELD_MOON,
                        icon_color=ft.colors.RED_300,
                        tooltip="تجميد الحساب ومحاصرة القيود",
                        disabled=is_frozen,
                        on_click=lambda _, cid=c["client_id"]: handle_status_change(cid, True)
                    ),
                    ft.IconButton(
                        icon=ft.icons.PLAY_ARROW_ROUNDED,
                        icon_color=ft.colors.GREEN_300,
                        tooltip="فك التجميد وإعادة التنشيط",
                        disabled=not is_frozen,
                        on_click=lambda _, cid=c["client_id"]: handle_status_change(cid, False)
                    )
                ], spacing=2)

                clients_table.rows.append(
                    ft.DataRow(
                        cells=[
                            ft.DataCell(ft.Text(c["client_id"], weight=ft.FontWeight.BOLD, color=ft.colors.BLUE_200)),
                            ft.DataCell(
                                ft.Column([
                                    ft.Text(c["business_name"], weight=ft.FontWeight.W_600, size=13),
                                    ft.Text(f"آخر مزامنة: {date_str}", size=10, color=ft.colors.GREY_500)
                                ], alignment=ft.MainAxisAlignment.CENTER, spacing=2)
                            ),
                            ft.DataCell(ft.Text(c["business_phone"], size=12)),
                            ft.DataCell(ft.Text(str(c["accounts_count"]), size=12)),
                            ft.DataCell(ft.Text(str(c["transactions_count"]), size=12)),
                            ft.DataCell(ft.Text(f"{c.get('data_size_kb', 0.0):.1f} KB", size=12, color=ft.colors.GREY_300)),
                            ft.DataCell(
                                ft.Row([
                                    ft.Icon(status_icon, color=status_color, size=14),
                                    ft.Text(status_text, size=12, color=status_color, weight=ft.FontWeight.BOLD)
                                ], spacing=6)
                            ),
                            ft.DataCell(action_buttons),
                        ]
                    )
                )
        page.update()

    # مستمع لأحداث كتابة البحث
    def on_search_changed(e):
        render_table(search_input.value)

    search_input.on_change = on_search_changed

    # تصميم بطاقة الإحصائيات الفردية
    def create_kpi_card(title, value_control, icon, color):
        return ft.Container(
            content=ft.Column([
                ft.Row([
                    ft.Icon(icon, color=color, size=24),
                    ft.Text(title, size=12, weight=ft.FontWeight.W_500, color=ft.colors.GREY_400),
                ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                ft.VerticalDivider(height=10, color=ft.colors.TRANSPARENT),
                value_control
            ], spacing=2),
            bgcolor=ft.colors.SURFACE_VARIANT,
            padding=16,
            border_radius=18,
            width=230,
            border=ft.border.all(1, ft.colors.WHITE10)
        )

    # الجزء العلوي من الواجهة (الهيدر والموديل)
    header_section = ft.Container(
        content=ft.Row([
            ft.Column([
                ft.Text("المحاسب anas برو - إدارة الفروع والتحكم الإداري", size=22, weight=ft.FontWeight.BOLD, color=ft.colors.PRIMARY),
                ft.Text("بوابة الأمان والتحكم بالعملاء، تراخيص الأجهزة، وصلاحيات الحسابات من نظام الويندوز ☁️", size=11, color=ft.colors.GREY_400),
            ], spacing=2),
            ft.Row([
                ft.ElevatedButton(
                    text="إضافة عميل يدوي",
                    icon=ft.icons.ADD_ROUNDED,
                    on_click=handle_add_client,
                    style=ft.ButtonStyle(
                        color=ft.colors.WHITE,
                        bgcolor=ft.colors.BLUE_700,
                        shape=ft.RoundedRectangleBorder(radius=10)
                    )
                ),
                ft.IconButton(
                    icon=ft.icons.REFRESH_ROUNDED,
                    icon_color=ft.colors.PRIMARY,
                    tooltip="تحديث قائمه الأجهزة",
                    on_click=lambda _: render_table(search_input.value)
                )
            ], spacing=10)
        ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
        padding=ft.padding.symmetric(horizontal=10, vertical=15)
    )

    # تجميع بطاقات الإحصائيات
    kpis_row = ft.Row([
        create_kpi_card("إجمالي العملاء المشتركين", total_users_card, ft.icons.PEOPLE_ROUNDED, ft.colors.BLUE_400),
        create_kpi_card("الأجهزة النشطة والمفعّلة", active_users_card, ft.icons.CHECK_CIRCLE_ROUNDED, ft.colors.GREEN_400),
        create_kpi_card("الحسابات والأجهزة المجمدة", frozen_users_card, ft.icons.GO_TO_LINE, ft.colors.RED_400),
        create_kpi_card("إجمالي حجم السحاب المشترك", vol_card, ft.icons.CLOUD_DONE_ROUNDED, ft.colors.AMBER_400),
    ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN)

    # صندوق البحث المتقدم والجدول
    table_card = ft.Container(
        content=ft.Column([
            ft.Row([
                ft.Text("جدول إدارة العملاء والاشتراكات الحية", size=14, weight=ft.FontWeight.BOLD),
                search_input
            ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
            ft.Divider(height=1, color=ft.colors.WHITE10),
            ft.Container(
                content=ft.ListView([clients_table], expand=True),
                height=320,
            )
        ], spacing=15),
        bgcolor=ft.colors.WHITE10,
        padding=20,
        border_radius=20,
        border=ft.border.all(1, ft.colors.WHITE10)
    )

    footer = ft.Container(
        content=ft.Row([
            ft.Text("تنبيه: يتم تحديث قائمة الأجهزة تلقائياً عند قيام أي موزع برفع كشف حساب من تطبيق الأندرويد Pro.", size=10, color=ft.colors.GREY_500),
            ft.Text("لوحة التحكم v2.1 • مبرمج لصالح إدارة المحاسب anas", size=10, color=ft.colors.GREY_500)
        ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
        padding=10
    )

    # تركيب شجرة الواجهة بالكامل داخل الصفحة
    page.add(
        ft.Column([
            header_section,
            ft.Divider(height=1, color=ft.colors.WHITE10),
            ft.VerticalDivider(height=10, color=ft.colors.TRANSPARENT),
            kpis_row,
            ft.VerticalDivider(height=10, color=ft.colors.TRANSPARENT),
            table_card,
            footer
        ], spacing=10, expand=True)
    )

    # تشغيل الإحصائيات والجدول لأول مرة
    calculate_stats()
    render_table()

if __name__ == "__main__":
    ft.app(target=main)
