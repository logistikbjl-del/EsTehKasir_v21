package com.estehkasir

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class Product(var id: Long,var name:String,var category:String,var price:Int,var stock:Int,var active:Boolean=true)
data class Sale(val time:String,val items:String,val total:Int,val paid:Int,val change:Int)

class MainActivity:Activity(){
 private val prefs by lazy{getSharedPreferences("estehkasir",MODE_PRIVATE)}
 private val products=mutableListOf<Product>(); private val sales=mutableListOf<Sale>()
 private val cart=linkedMapOf<Long,Int>(); private val rupiah=NumberFormat.getCurrencyInstance(Locale("id","ID"))
 private val df=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("id","ID")); private var page="dashboard"

 override fun onCreate(b:Bundle?){super.onCreate(b);load();dashboard()}
 private fun load(){
  products.clear(); val r=prefs.getString("products",null)
  if(r.isNullOrBlank()){products.addAll(listOf(Product(1,"Es Teh Jumbo","Es Teh",8000,100),Product(2,"Es Jeruk","Minuman",7000,100),Product(3,"Matcha","Minuman",12000,100),Product(4,"Thai Tea","Minuman",10000,100),Product(5,"Green Tea","Minuman",10000,100)));saveP()}
  else r.split("||").filter{it.isNotBlank()}.forEach{val x=it.split("|");if(x.size>=6)products.add(Product(x[0].toLong(),x[1],x[2],x[3].toInt(),x[4].toInt(),x[5]=="1"))}
  sales.clear();prefs.getString("sales",null)?.split("||")?.filter{it.isNotBlank()}?.forEach{val x=it.split("|");if(x.size>=5)sales.add(Sale(x[0],x[1],x[2].toInt(),x[3].toInt(),x[4].toInt()))}
 }
 private fun saveP(){prefs.edit().putString("products",products.joinToString("||"){"${it.id}|${it.name}|${it.category}|${it.price}|${it.stock}|${if(it.active)1 else 0}"}).apply()}
 private fun saveS(){prefs.edit().putString("sales",sales.joinToString("||"){"${it.time}|${it.items}|${it.total}|${it.paid}|${it.change}"}).apply()}
 private fun root():ScrollView{val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,24,24,32);setBackgroundColor(Color.rgb(248,250,248))};return ScrollView(this).apply{addView(l)}}
 private fun box(s:ScrollView)=s.getChildAt(0) as LinearLayout
 private fun title(t:String)=TextView(this).apply{text=t;textSize=25f;setTextColor(Color.rgb(35,85,45));setPadding(0,0,0,18)}
 private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;setOnClickListener{a()}}
 private fun shop()=prefs.getString("shop","Toko Es Teh")?:"Toko Es Teh"

 private fun dashboard(){page="dashboard";val r=root();val b=box(r);b.addView(title("🧋 Es Teh Kasir v2.1"));b.addView(TextView(this).apply{text="${shop()} • Kasir offline";textSize=15f})
  b.addView(button("🛒 KASIR"){cashier()});b.addView(button("📦 KELOLA PRODUK"){products()});b.addView(button("📊 LAPORAN HARIAN"){report()});b.addView(button("🧾 RIWAYAT TRANSAKSI"){history()});b.addView(button("⚙️ NAMA TOKO"){shopDialog()});b.addView(button("💾 BACKUP DATA"){backup()});setContentView(r)}
 private fun cashier(){page="cashier";cart.clear();val r=root();val b=box(r);b.addView(title("🛒 Kasir"));val q=EditText(this).apply{hint="🔎 Cari minuman...";setSingleLine()};b.addView(q)
  val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(list);val cb=TextView(this).apply{textSize=16f};val tb=TextView(this).apply{textSize=20f;setTextColor(Color.rgb(35,85,45))};val pay=EditText(this).apply{hint="Uang diterima (Rp)";inputType=2}
  fun refresh(){list.removeAllViews();val x=q.text.toString().lowercase();products.filter{it.active&&it.name.lowercase().contains(x)}.forEach{p->list.addView(button("${p.name}\n${rupiah.format(p.price)} • Stok ${p.stock}"){val n=cart[p.id]?:0;if(n<p.stock)cart[p.id]=n+1 else toast("Stok habis");refreshCart()})}}
  fun refreshCart(){val lines=cart.mapNotNull{(id,n)->products.find{it.id==id}?.let{"• ${it.name} x$n = ${rupiah.format(it.price*n)}"}};val total=cart.entries.sumOf{(id,n)->products.find{it.id==id}?.price?.times(n)?:0};cb.text=if(lines.isEmpty())"Keranjang kosong" else "Pesanan:\n"+lines.joinToString("\n");tb.text="TOTAL: ${rupiah.format(total)}"}
  q.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){};override fun onTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){refresh()};override fun afterTextChanged(e:Editable?){} })
  b.addView(cb);b.addView(tb);b.addView(pay);b.addView(button("🧾 SELESAIKAN TRANSAKSI"){val total=cart.entries.sumOf{(id,n)->products.find{it.id==id}?.price?.times(n)?:0};val paid=pay.text.toString().toIntOrNull()?:0;if(total==0)toast("Keranjang kosong")else if(paid<total)toast("Pembayaran kurang")else{cart.forEach{(id,n)->products.find{it.id==id}?.let{it.stock-=n}};val items=cart.mapNotNull{(id,n)->products.find{it.id==id}?.let{"${it.name} x$n"}}.joinToString(", ");val s=Sale(df.format(Date()),items,total,paid,paid-total);sales.add(0,s);saveP();saveS();cart.clear();pay.text.clear();refreshCart();receipt(s)}});b.addView(button("Kosongkan Keranjang"){cart.clear();refreshCart()});b.addView(button("← Kembali"){dashboard()});refresh();refreshCart();setContentView(r)}
 private fun products(){page="products";val r=root();val b=box(r);b.addView(title("📦 Kelola Produk"));b.addView(button("➕ Tambah Minuman"){dialogProduct(null)})
  products.forEach{p->val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,10,0,10)};l.addView(TextView(this).apply{text="${p.name}\n${p.category} • ${rupiah.format(p.price)} • Stok ${p.stock}\n${if(p.active)"Aktif" else "Nonaktif"}";textSize=16f});l.addView(button("✏️ Edit"){dialogProduct(p)});l.addView(button(if(p.active)"🔴 Nonaktifkan" else "🟢 Aktifkan"){p.active=!p.active;saveP();products()});l.addView(button("🗑️ Hapus"){products.removeIf{it.id==p.id};saveP();products()});b.addView(l)};b.addView(button("← Kembali"){dashboard()});setContentView(r)}
 private fun dialogProduct(p:Product?){val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,10,40,10)};val n=EditText(this).apply{hint="Nama minuman";setText(p?.name?:"")};val c=EditText(this).apply{hint="Kategori";setText(p?.category?:"Minuman")};val pr=EditText(this).apply{hint="Harga (Rp)";inputType=2;setText(p?.price?.toString()?:"")};val st=EditText(this).apply{hint="Stok";inputType=2;setText(p?.stock?.toString()?:"100")};listOf(n,c,pr,st).forEach{l.addView(it)}
  AlertDialog.Builder(this).setTitle(if(p==null)"Tambah Minuman" else "Edit Minuman").setView(l).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->val price=pr.text.toString().toIntOrNull()?:0;if(n.text.isNotBlank()&&price>0){if(p==null)products.add(Product(System.currentTimeMillis(),n.text.toString().trim(),c.text.toString().ifBlank{"Minuman"},price,st.text.toString().toIntOrNull()?:0))else{p.name=n.text.toString().trim();p.category=c.text.toString().ifBlank{"Minuman"};p.price=price;p.stock=st.text.toString().toIntOrNull()?:0};saveP();products()}}.show()}
 private fun report(){page="report";val r=root();val b=box(r);b.addView(title("📊 Laporan Harian"));val d=SimpleDateFormat("dd/MM/yyyy",Locale("id","ID")).format(Date());val ss=sales.filter{it.time.startsWith(d)};b.addView(TextView(this).apply{text="Tanggal: $d\nTransaksi: ${ss.size}\nOmzet: ${rupiah.format(ss.sumOf{it.total})}";textSize=18f});ss.forEach{b.addView(TextView(this).apply{text="${it.time}\n${it.items}\n${rupiah.format(it.total)}";textSize=16f;setPadding(0,12,0,12)})};b.addView(button("← Kembali"){dashboard()});setContentView(r)}
 private fun history(){page="history";val r=root();val b=box(r);b.addView(title("🧾 Riwayat Transaksi"));if(sales.isEmpty())b.addView(TextView(this).apply{text="Belum ada transaksi.";textSize=17f})else sales.forEach{s->b.addView(button("${s.time}\n${s.items}\nTOTAL ${rupiah.format(s.total)}"){receipt(s)})};b.addView(button("← Kembali"){dashboard()});setContentView(r)}
 private fun receipt(s:Sale){val text="${shop()}\nES TEH KASIR v2.1\n----------------\n${s.time}\n${s.items}\n\nTOTAL: ${rupiah.format(s.total)}\nBAYAR: ${rupiah.format(s.paid)}\nKEMBALI: ${rupiah.format(s.change)}";AlertDialog.Builder(this).setTitle("🧾 Struk Transaksi").setMessage(text).setNegativeButton("Tutup",null).setPositiveButton("Bagikan"){_,_->startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Bagikan struk"))}.show()}
 private fun shopDialog(){val e=EditText(this).apply{setText(shop());hint="Nama toko"};AlertDialog.Builder(this).setTitle("🏪 Nama Toko").setView(e).setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->prefs.edit().putString("shop",e.text.toString().trim()).apply();dashboard()}.show()}
 private fun backup(){val d=buildString{append("BACKUP ES TEH KASIR v2.1\nToko: ${shop()}\n\nPRODUK\n");products.forEach{append("${it.id}|${it.name}|${it.category}|${it.price}|${it.stock}|${it.active}\n")};append("\nTRANSAKSI\n");sales.forEach{append("${it.time}|${it.items}|${it.total}|${it.paid}|${it.change}\n")}};startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,d)},"Backup / Bagikan data"))}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 override fun onBackPressed(){if(page!="dashboard")dashboard()else super.onBackPressed()}
}
