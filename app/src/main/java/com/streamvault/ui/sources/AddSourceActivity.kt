package com.streamvault.ui.sources
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.streamvault.data.model.SavedSource
import com.streamvault.data.repository.MainRepository
import com.streamvault.databinding.ActivityAddSourceBinding
import java.util.UUID
class AddSourceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSourceBinding
    private lateinit var repo: MainRepository
    private val PICK_FILE = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSourceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = MainRepository(getSharedPreferences("streamvault", Context.MODE_PRIVATE))
        binding.btnBack.setOnClickListener { finish() }
        setupTabs(); setupButtons(); loadExistingSources()
    }
    private fun setupTabs() {
        binding.tabM3uUrl.setOnClickListener { showTab(0) }
        binding.tabXtream.setOnClickListener { showTab(1) }
        binding.tabFile.setOnClickListener { showTab(2) }
        showTab(0)
    }
    private fun showTab(tab: Int) {
        binding.panelM3uUrl.visibility = if (tab==0) View.VISIBLE else View.GONE
        binding.panelXtream.visibility = if (tab==1) View.VISIBLE else View.GONE
        binding.panelFile.visibility = if (tab==2) View.VISIBLE else View.GONE
        binding.tabM3uUrl.isSelected=tab==0; binding.tabXtream.isSelected=tab==1; binding.tabFile.isSelected=tab==2
    }
    private fun setupButtons() {
        binding.btnAddM3u.setOnClickListener {
            val name=binding.etM3uName.text.toString().trim(); val url=binding.etM3uUrl.text.toString().trim()
            if (name.isEmpty()||url.isEmpty()) { toast("Completa todos los campos"); return@setOnClickListener }
            repo.saveSource(SavedSource(name=name, type="M3U_URL", m3uUrl=url)); toast("Lista M3U agregada"); loadExistingSources()
        }
        binding.btnAddXtream.setOnClickListener {
            val name=binding.etXtreamName.text.toString().trim(); val url=binding.etXtreamUrl.text.toString().trim()
            val user=binding.etXtreamUser.text.toString().trim(); val pass=binding.etXtreamPass.text.toString().trim()
            if (name.isEmpty()||url.isEmpty()||user.isEmpty()||pass.isEmpty()) { toast("Completa todos los campos"); return@setOnClickListener }
            repo.saveSource(SavedSource(name=name, type="XTREAM", xtreamUrl=url, xtreamUser=user, xtreamPass=pass))
            toast("Fuente Xtream agregada"); loadExistingSources()
        }
        binding.btnPickFile.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type="*/*" }, PICK_FILE)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_FILE && resultCode==Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val name = binding.etFileName.text.toString().trim().ifEmpty { uri.lastPathSegment ?: "M3U Local" }
            repo.saveSource(SavedSource(name=name, type="M3U_FILE", localFilePath=uri.toString()))
            toast("Archivo M3U agregado"); loadExistingSources()
        }
    }
    private fun loadExistingSources() {
        val srcs = repo.getSources()
        binding.tvSourcesList.text = if (srcs.isEmpty()) "Sin fuentes" else srcs.joinToString("\n") { "• ${it.name} (${it.type})" }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
