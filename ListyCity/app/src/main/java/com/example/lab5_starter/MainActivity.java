package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private EditText editTextCityName;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    private static final String TAG = "Firestore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);
        editTextCityName = findViewById(R.id.editTextCityName);
        cityListView = findViewById(R.id.listviewCities);

        // Create city array and adapter
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Load cities from Firestore with real-time listener
        citiesRef.addSnapshotListener((querySnapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to collection", error);
                return;
            }
            if (querySnapshots != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot doc : querySnapshots) {
                    String cityName = doc.getId();
                    String province = doc.getString("province");
                    cityArrayList.add(new City(cityName, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // Add City button listener
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Delete City button listener
        deleteCityButton.setOnClickListener(view -> {
            String cityName = editTextCityName.getText().toString().trim();
            if (!cityName.isEmpty()) {
                deleteCity(cityName);
                editTextCityName.setText("");
            }
        });

        // Item click listener for editing
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });
    }

    @Override
    public void addCity(City city) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("province", city.getProvince());

        citiesRef.document(city.getName())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City added successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding city", e));
    }

    @Override
    public void updateCity(City city, String newName, String newProvince) {
        // Delete old document and create new one (doc ID = city name)
        citiesRef.document(city.getName()).delete();

        HashMap<String, Object> data = new HashMap<>();
        data.put("province", newProvince);
        citiesRef.document(newName).set(data);
    }

    private void deleteCity(String cityName) {
        citiesRef.document(cityName)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City deleted successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting city", e));
    }
}
