#!/bin/bash
sed -i "/db?.execSQL(\"INSERT INTO commands (pattern, action, parameters) VALUES ('play reels', 'REELS_SCROLL', '')\")/a \        db?.execSQL(\"INSERT INTO commands (pattern, action, parameters) VALUES ('fake call', 'FAKE_CALL', '')\")" app/src/main/java/com/kyro/brain/RawBrainQuery.kt
