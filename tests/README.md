# Tests
- `TestLogic.java` — JVM unit tests for Geo/Zone/ZoneStore/Prefs (run: `javac -cp json.jar -d out stubs/android/content/*.java ../app/src/com/aurora/hush/{Geo,Zone,ZoneStore,Prefs}.java TestLogic.java && java -cp out:json.jar TestLogic`)
- `static_check.py` — verifies every R.* / layout / manifest reference exists in the built APK (run: `python3 static_check.py ../release/Hush-v1.1.0.apk`)
