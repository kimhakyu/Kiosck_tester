package com.example.kiosk

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kiosk.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterFragment: Fragment(R.layout.fragment_register) {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var businessService: BusinessService
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val apiKey = "UJCriUTfHckUGiN6DRxSOPSutu7rP6fA3K7oZIkXB3azu02+PzreiRVz6/Gexnqj8On7FHIZTqO+L/XMve/Wxw=="
    private var businessNumberCK = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRegisterBinding.bind(view)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        businessService = RetrofitClient.getInstance().create(BusinessService::class.java)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.registerButton.isEnabled = businessNumberCK && (binding.passwordEditText.text.toString() == binding.passwordCKEditText.text.toString())
                Log.e("textWather: ${binding.registerButton.isEnabled}", "")
            }

            override fun afterTextChanged(p0: Editable?) {}
        }

        binding.passwordEditText.addTextChangedListener(textWatcher)
        binding.passwordCKEditText.addTextChangedListener(textWatcher)


        binding.registerButton.setOnClickListener {
            var name = binding.nameEditText.text.toString()
            var email = binding.emailEditText.text.toString()
            var password = binding.passwordEditText.text.toString()
            var tradeName = binding.tradeNameEditText.text.toString()
            var businessnumber = binding.businessNumberEditText.text.toString()
            var address = binding.AddressEditText.text.toString()

            registerUser(name ,email, password, tradeName, businessnumber, address)

        }

        binding.businessButton.setOnClickListener {
            val businessNumber = binding.businessNumberEditText.text.toString()
            checkBusinessNumber(businessNumber)
        }
    }

    private fun registerUser(name: String, email: String, password: String, tradeName: String, businessnumber: String, address: String) {
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveAdminDataToFirestore(name, email, password, tradeName, businessnumber, address)
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveAdminDataToFirestore(name: String, email: String, password: String, tradeName: String, businessnumber: String, address: String) {
        val admin = AdminData(
            name = name,
            email = email,
            tradeName = tradeName,
            businessnumber = businessnumber,
            address = address

        )

        firestore.collection("admin").document(email)
            .set(admin)
            .addOnSuccessListener {
                Toast.makeText(context, "회원가입 완료", Toast.LENGTH_SHORT).show()
            }. addOnFailureListener {
                Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkBusinessNumber(businessNumber: String) {
        val request = BusinessRequest(b_no = listOf(businessNumber))
        Log.e("request", request.toString())
        businessService.checkBusinessNumber(apiKey, request).enqueue(object : Callback<BusinessResponse> {
            override fun onResponse(
                call: Call<BusinessResponse>,
                response: Response<BusinessResponse>
            ) {
                if (response.isSuccessful) {
                    val businessData = response.body()?.data?.firstOrNull()
                    val errorBody = response.errorBody()?.string()
                    Log.e("API ERRor: $errorBody", "dd")
                    if(businessData?.b_stt_cd == "01") {
                        Toast.makeText(requireContext(),"등록된 사업자 번호", Toast.LENGTH_SHORT).show()
                        //binding.registerButton.setEnabled(true)
                        businessNumberCK = true
                    } else {
                        Toast.makeText(requireContext(),"유효하지 않은 등록번호", Toast.LENGTH_SHORT).show()
                    }
                } else{
                    Toast.makeText(requireContext(),"요청실패 ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "요청실패 ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BusinessResponse>, t: Throwable) {
                Toast.makeText(requireContext(),"네트워크 오류 발생 ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })


    }
}