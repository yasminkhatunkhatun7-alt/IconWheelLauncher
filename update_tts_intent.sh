#!/bin/bash
sed -i 's/val ttsIntent = Intent("com.kyro.ACTION_TTS")/val appModule = (application as com.kyro.KyroApplication).appModule\n                                appModule.ttsEngine.speak("Sir, page nahi mila.")/g' app/src/main/java/com/kyro/service/KyroAccessibilityService.kt
sed -i '/ttsIntent.putExtra("text", "Sir, page nahi mila.")/d' app/src/main/java/com/kyro/service/KyroAccessibilityService.kt
sed -i '/sendBroadcast(ttsIntent)/d' app/src/main/java/com/kyro/service/KyroAccessibilityService.kt
