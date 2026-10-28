package com.mclauncher.android.ui.instances

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mclauncher.android.R
import com.mclauncher.android.data.repository.InstanceRepository

class InstancesFragment : Fragment() {

    private lateinit var instanceRepository: InstanceRepository
    private lateinit var instancesList: ListView
    private lateinit var emptyText: TextView
    private var instances: List<com.mclauncher.android.data.model.Instance> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_instances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        instanceRepository = InstanceRepository(requireContext())

        instancesList = view.findViewById(R.id.instances_list)
        emptyText = view.findViewById(R.id.empty_text)
        val btnCreate = view.findViewById<Button>(R.id.btn_create_instance)

        btnCreate.setOnClickListener { showCreateInstanceDialog() }

        loadInstances()
    }

    override fun onResume() {
        super.onResume()
        loadInstances()
    }

    private fun loadInstances() {
        instances = instanceRepository.listInstances()
        if (instances.isEmpty()) {
            instancesList.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
            instancesList.visibility = View.VISIBLE

            val displayItems = instances.map { "${it.name} (v${it.versionId})" }
            val adapter = ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_2,
                displayItems
            )
            instancesList.adapter = adapter

            instancesList.setOnItemClickListener { _, _, position, _ ->
                val selected = instances[position]
                Toast.makeText(requireContext(), "Instance: ${selected.name}", Toast.LENGTH_SHORT).show()
            }

            instancesList.setOnItemLongClickListener { _, _, position, _ ->
                val selected = instances[position]
                showDeleteDialog(selected)
                true
            }
        }
    }

    private fun showCreateInstanceDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Create Instance")

        val input = EditText(requireContext())
        input.hint = "Instance name"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        dialog.setView(input)

        val versions = listOf("1.21", "1.20.1", "1.19.4", "1.18.2") // placeholder
        val versionSpinner = Spinner(requireContext())
        versionSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, versions)
        dialog.setPositiveButton("Create") { _, _ ->
            val name = input.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val versionId = versionSpinner.selectedItem.toString()
            instanceRepository.createInstance(name, versionId, "none")
            Toast.makeText(requireContext(), "Instance '$name' created", Toast.LENGTH_SHORT).show()
            loadInstances()
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun showDeleteDialog(instance: com.mclauncher.android.data.model.Instance) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Instance")
            .setMessage("Delete '${instance.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                if (instanceRepository.deleteInstance(instance)) {
                    Toast.makeText(requireContext(), "Instance deleted", Toast.LENGTH_SHORT).show()
                    loadInstances()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete instance", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
