package net.cyclestreets.track;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import net.cyclestreets.util.ListFactory;

public class SaveTrip extends Activity
    implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
  public static void start(final Context context, final long tripid) {
    final Intent fi = new Intent(context, SaveTrip.class);
    fi.putExtra("showtrip", tripid);
    context.startActivity(fi);
  } // start

  public static void startWithUnsaved(final Context context) {
    final int unfinishedTrip = DbAdapter.unfinishedTrip(context);
    start(context, unfinishedTrip);
  } // startWithUnsaved

  private final Map<Integer, ToggleButton> purpButtons = new HashMap<>();
  private final Map <Integer, String> purpDescriptions = new HashMap<>();
  private TripData trip_;
  private String purpose_;
  private Spinner age_;
  private Spinner gender_;
  private Spinner experience_;
  private SharedPreferences prefs_;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.save);

    prefs_ = getSharedPreferences("PersonalInfo", Application.MODE_PRIVATE);

    final Bundle cmds = getIntent().getExtras();
    final long journeyId = cmds.getLong("showtrip");
    trip_ = TripData.fetchTrip(this, journeyId);

    // Set up trip purpose buttons
    purpose_ = "";
    setupPurposeButtons();

    // Discard btn
    final Button btnDiscard = viewById(R.id.ButtonDiscard);
    btnDiscard.setOnClickListener(this);

    // Submit btn
    final Button btnSubmit = viewById(R.id.ButtonSubmit);
    btnSubmit.setOnClickListener(this);
    btnSubmit.setEnabled(false);

    age_ = viewById(R.id.age);
    setupAge(age_);

    gender_ = viewById(R.id.gender);
    setupGender(gender_);

    experience_ = viewById(R.id.experience);
    setupExperience(experience_);

    // Don't pop up the soft keyboard until user clicks!
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  } // onCreate

  private <T> T viewById(final int id) { return (T)findViewById(id); }

  public void onClick(final View v) {
    if(v.getId() == R.id.ButtonDiscard)
      discardTrip();

    if(v.getId() == R.id.ButtonSubmit)
      uploadTrip();
  } // onClick

  private void discardTrip() {
    Toast.makeText(getBaseContext(), R.string.savetrip_discarded, Toast.LENGTH_SHORT).show();

    trip_.dropTrip();

    //CycleHackney.start(this);
    finish();
  } // discardTrip

  private void uploadTrip() {
    if (purpose_.equals("")) {
      // Oh no!  No trip purpose!
      Toast.makeText(getBaseContext(), R.string.savetrip_no_purpose, Toast.LENGTH_SHORT).show();
      return;
    }

    EditText notes = (EditText)findViewById(R.id.NotesField);

    String fancyStartTime = DateFormat.getInstance().format(trip_.startTime()*1000);

    // "3.5 miles in 26 minutes"
    final long minutes = trip_.secondsElapsed() / 60;
    String fancyEndInfo = getString(R.string.savetrip_end_info_format,
                                    trip_.distanceTravelled(),
                                    minutes,
                                    notes.getEditableText().toString());

    // Save the trip details to the phone database. W00t!
    trip_.updateTrip(purpose_,
                     fancyStartTime,
                     fancyEndInfo,
                     notes.getEditableText().toString(),
                     age_.getSelectedItem().toString(),
                     gender_.getSelectedItem().toString(),
                     experience_.getSelectedItem().toString());
    trip_.metaDataComplete();

    SharedPreferences.Editor e = prefs_.edit();
    e.putInt("age", age_.getSelectedItemPosition());
    e.putInt("gender", gender_.getSelectedItemPosition());
    e.putInt("experience", experience_.getSelectedItemPosition());
    e.commit();

    TripDataUploader.upload(this, trip_);

    //CycleHackney.start(this);
    finish();
  } // uploadTrip

  private void setupAge(final Spinner age) {
    final List<String> ages = ListFactory.list(getString(R.string.savetrip_please_select),
                                               "0-10",
                                               "11-16",
                                               "17-24",
                                               "25-44",
                                               "45-64",
                                               "65-74",
                                               "75-84",
                                               "85+");
    age.setAdapter(new SpinnerList(this, ages));
    int index = prefs_.getInt("age", 0);
    age.setSelection(index);
    age.setOnItemSelectedListener(this);
  } // setupAge

  private void setupGender(final Spinner gender) {
    final List<String> genders = ListFactory.list(getString(R.string.savetrip_please_select),
                                                  getString(R.string.savetrip_gender_male),
                                                  getString(R.string.savetrip_gender_female),
                                                  getString(R.string.savetrip_gender_private));
    gender.setPrompt(getString(R.string.savetrip_gender_prompt));
    gender.setAdapter(new SpinnerList(this, genders));
    int index = prefs_.getInt("gender", 0);
    gender.setSelection(index);
    gender.setOnItemSelectedListener(this);
  } // setupGender

  private void setupExperience(final Spinner experience) {
    final List<String> experienceLevels = ListFactory.list(getString(R.string.savetrip_please_select),
                                                           getString(R.string.savetrip_experience_experienced),
                                                           getString(R.string.savetrip_experience_infrequent),
                                                           getString(R.string.savetrip_experience_beginner));
    experience.setPrompt(getString(R.string.savetrip_experience_prompt));
    experience.setAdapter(new SpinnerList(this, experienceLevels));
    int index = prefs_.getInt("experience", 0);
    experience.setSelection(index);
    experience.setOnItemSelectedListener(this);
  } // setupExperience

  private void setupPurposeButtons() {
    purpButtons.put(R.id.ToggleCommute, (ToggleButton)findViewById(R.id.ToggleCommute));
    purpButtons.put(R.id.ToggleSchool,  (ToggleButton)findViewById(R.id.ToggleSchool));
    purpButtons.put(R.id.ToggleWorkRel, (ToggleButton)findViewById(R.id.ToggleWorkRel));
    purpButtons.put(R.id.ToggleExercise,(ToggleButton)findViewById(R.id.ToggleExercise));
    purpButtons.put(R.id.ToggleSocial,  (ToggleButton)findViewById(R.id.ToggleSocial));
    purpButtons.put(R.id.ToggleShopping,(ToggleButton)findViewById(R.id.ToggleShopping));
    purpButtons.put(R.id.ToggleErrand,  (ToggleButton)findViewById(R.id.ToggleErrand));
    purpButtons.put(R.id.ToggleOther,   (ToggleButton)findViewById(R.id.ToggleOther));

    purpDescriptions.put(R.id.ToggleCommute, getString(R.string.savetrip_purpose_commute));
    purpDescriptions.put(R.id.ToggleSchool, getString(R.string.savetrip_purpose_school));
    purpDescriptions.put(R.id.ToggleWorkRel, getString(R.string.savetrip_purpose_work_related));
    purpDescriptions.put(R.id.ToggleExercise, getString(R.string.savetrip_purpose_exercise));
    purpDescriptions.put(R.id.ToggleSocial, getString(R.string.savetrip_purpose_social));
    purpDescriptions.put(R.id.ToggleShopping, getString(R.string.savetrip_purpose_shopping));
    purpDescriptions.put(R.id.ToggleErrand, getString(R.string.savetrip_purpose_errand));
    purpDescriptions.put(R.id.ToggleOther, getString(R.string.savetrip_purpose_other));

    for (Entry<Integer, ToggleButton> e: purpButtons.entrySet())
      e.getValue().setOnCheckedChangeListener(this);
  } // preparePurposeButtons

  @Override
  public void onCheckedChanged(CompoundButton v, boolean isChecked) {
    if (!isChecked)
      return;

    for (Entry<Integer, ToggleButton> e: purpButtons.entrySet())
      e.getValue().setChecked(false);

    v.setChecked(true);
    purpose_ = v.getText().toString();
    ((TextView)findViewById(R.id.TextPurpDescription)).setText(
       Html.fromHtml(purpDescriptions.get(v.getId())));

    enableSubmit();
  } // onCheckedChanged

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    enableSubmit();
  } // onItemClick
  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {
    enableSubmit();
  } // onItemClick

  private void enableSubmit() {
    boolean enabled = false;
    for (Entry<Integer, ToggleButton> e: purpButtons.entrySet())
      enabled |= e.getValue().isChecked();

    if (!enabled)
      return;

    final Button btnSubmit = (Button)findViewById(R.id.ButtonSubmit);
    btnSubmit.setEnabled((age_.getSelectedItemPosition() != 0 &&
                          gender_.getSelectedItemPosition() != 0 &&
                          experience_.getSelectedItemPosition() != 0));
  } // enabledSubmit

  ///////////////////////
  static private class SpinnerList extends BaseAdapter {
    private final LayoutInflater inflater_;
    private final List<String> list_;

    public SpinnerList(final Context context, final List<String> list) {
      inflater_ = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      list_ = list;
    } // CategoryAdapter

    @Override
    public int getCount() { return list_.size(); }
    @Override
    public String getItem(final int position) { return list_.get(position); } // getItem
    @Override
    public long getItemId(final int position) { return position; } // getItemId

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
      final int id = (parent instanceof Spinner) ? android.R.layout.simple_spinner_item : android.R.layout.simple_spinner_dropdown_item;
      final TextView tv = (TextView)inflater_.inflate(id, parent, false);
      tv.setText(getItem(position));
      return tv;
    } // getView
  } // SpinnerList

} // SaveTrip
