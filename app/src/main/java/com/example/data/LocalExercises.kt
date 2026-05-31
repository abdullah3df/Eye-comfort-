package com.example.data

object LocalExercises {
    // Represents a beautifully preloaded Eye Exercise matching the expected Gemini output format
    data class FallbackExercise(
        val title: String,
        val steps: String,
        val durationSeconds: Int,
        val benefit: String
    )

    // A map of lists of scientifically-backed eye exercises customized per language
    private val exercisesByLanguage = mapOf(
        "en" to listOf(
            FallbackExercise(
                title = "The 20-20-20 Rule",
                steps = "Every 20 minutes, look away from your screen. Focus on an object at least 20 feet away for a full 20 seconds.",
                durationSeconds = 20,
                benefit = "Allows the ciliary muscles inside your eyes to fully relax, reducing focusing strain from long-term screen depth."
            ),
            FallbackExercise(
                title = "Ocular Palming",
                steps = "Rub your hands together vigorously to generate gentle heat. Place your warm palms over your closed eyes without applying pressure, breathing deeply.",
                durationSeconds = 60,
                benefit = "Darkness combined with soothing heat relaxes hyperactive optical nerves, lubricates the eyes, and relieves visual fatigue."
            ),
            FallbackExercise(
                title = "Conscious Rapid Blinking",
                steps = "Blink quickly and softly 10 to 15 times over 20 seconds. Then close your eyes and relax for another 10 seconds. Repeat twice.",
                durationSeconds = 30,
                benefit = "Refreshes the tear film, lubricates dry retinas, and prevents eye irritation that occurs because we blink 50% less when looking at screens."
            ),
            FallbackExercise(
                title = "Visual Figure 8",
                steps = "Imagine a giant figure 8 on the floor or wall ten feet in front of you. Trace this figure slowly with your eyes for 30 seconds, then reverse the direction.",
                durationSeconds = 60,
                benefit = "Exercises the external extraocular muscles, improving eye flexibility, fluid tracking, and range of movement."
            ),
            FallbackExercise(
                title = "Ocular Focus Shift",
                steps = "Hold your thumb out 6 inches from your nose. Focus on your thumb, then shift view to a distant object across the room. Alternate steady views every 5 seconds.",
                durationSeconds = 40,
                benefit = "Strengthens and tests focal flexibility, stretching the lens and accommodation muscles of the eye."
            ),
            FallbackExercise(
                title = "Clockwork Eye Rolling",
                steps = "Sit up straight. Slowly roll your eyes in a full circle clockwise 5 times, then close your eyes for 5 seconds. Repeat counter-clockwise.",
                durationSeconds = 30,
                benefit = "Soothes optic tension, targets stiff ocular muscle groups, and balances eye pressure."
            )
        ),
        "ar" to listOf(
            FallbackExercise(
                title = "قاعدة 20-20-20",
                steps = "كل 20 دقيقة، ارفع نظرك عن الشاشة. ركز على شيء يبعد عنك 20 قدمًا على الأقل (حوالي 6 أمتار) لمدة 20 ثانية كاملة.",
                durationSeconds = 20,
                benefit = "يسمح للعضلات الهدبية داخل العين بالارتخاء التام، مما يقلل من إجهاد التركيز المستمر على الشاشات."
            ),
            FallbackExercise(
                title = "راحة راحة اليد (Palming)",
                steps = "افرك كفيك معًا بقوة لتوليد حرارة لطيفة. ضع راحتي يديك الدافئتين فوق عينيك المغمضتين دون الضغط عليهما، وتنفّس بعمق.",
                durationSeconds = 60,
                benefit = "يساعد الظلام الدامس والحرارة اللطيفة على تهدئة الأعصاب البصرية واسترخائها، وتخفيف التعب البصري المتراكم."
            ),
            FallbackExercise(
                title = "الرمش السريع والواعي",
                steps = "ارمِش بعينيك بسرعة ولطف من 10 إلى 15 مرة خلال 20 ثانية. ثم أغمض عينيك واسترخِ لمدة 10 ثوانٍ أخرى. كرر العملية مرتين.",
                durationSeconds = 30,
                benefit = "يعيد إنتاج وتوزيع الغشاء الدمعي المرطب للعين، ويمنع الجفاف والاحمرار الناتج عن قلة الرمش أثناء استخدام الأجهزة."
            ),
            FallbackExercise(
                title = "رسم رقم 8 بالعين",
                steps = "تخيل رقم 8 كبير مستلقٍ جانبيًا على بعد 3 أمتار منك. تتبع هذا المسار ببطء بعينيك لمدة 30 ثانية في اتجاه واحد، ثم بالاتجاه المعاكس.",
                durationSeconds = 60,
                benefit = "يمد العضلات الخارجية المحيطة بالعين، مما يحسن من مرونة العين والقدرة على التتبع السلس للمشاهد البصرية."
            ),
            FallbackExercise(
                title = "تغيير التركيز البؤري",
                steps = "ضع إبهامك على بعد 15 سم أمام أنفك. ركز نظرك عليه لثوانٍ، ثم غير نظرك لتركز على جسم بعيد في الغرفة. تنقل بينهما كل 5 ثوانٍ.",
                durationSeconds = 40,
                benefit = "يقوي ويحسن من مرونة العدسة البؤرية داخل العين، ويدرب العضلات المسؤولة عن تكييف الرؤية."
            ),
            FallbackExercise(
                title = "دحرجة كرة العين",
                steps = "اجلس باعتدال وتطلع للأمام. دحرج عينيك ببطء في دائرة كاملة باتجاه عقارب الساعة 5 مرات، ثم أغمض عينك لـ 5 ثوانٍ. كررها بالاتجاه المعاكس.",
                durationSeconds = 30,
                benefit = "يخفف من تشنج وشد عضلات العين المتصلبة جراء التحديق الطويل في زوايا الشاشة الضيقة."
            )
        ),
        "fr" to listOf(
            FallbackExercise(
                title = "La Règle des 20-20-20",
                steps = "Toutes les 20 minutes, détournez le regard de votre écran. Fixez un objet situé à au moins 20 pieds (6 mètres) pendant 20 secondes entières.",
                durationSeconds = 20,
                benefit = "Permet aux muscles ciliaires à l'intérieur de vos yeux de se détendre pleinement, réduisant ainsi la fatigue d'accommodation."
            ),
            FallbackExercise(
                title = "Le Palming Oculaire",
                steps = "Frottez vigoureusement vos mains l'une contre l'autre pour générer de la chaleur. Placez vos paumes chaudes sur vos yeux fermés sans appuyer, en respirant profondément.",
                durationSeconds = 60,
                benefit = "L'obscurité associée à une chaleur douce apaise les nerfs optiques hyperactifs, lubrifie les yeux et soulage la fatigue visuelle."
            ),
            FallbackExercise(
                title = "Clignement Conscient Rapide",
                steps = "Clignez des yeux rapidement et doucement 10 à 15 fois pendant 20 secondes. Fermez ensuite vos yeux pour vous détendre 10 secondes. Répétez deux fois.",
                durationSeconds = 30,
                benefit = "Régénère le film lacrymal, réhydrate les rétines desséchées et évite les irritations dues au manque de clignements devant un écran."
            ),
            FallbackExercise(
                title = "Le 8 Visuel Astigmate",
                steps = "Imaginez un grand chiffre 8 couché sur le sol à 3 mètres de vous. Tracez-le lentement avec vos yeux pendant 30 secondes, puis changez de sens.",
                durationSeconds = 60,
                benefit = "Fait travailler les muscles extra-oculaires externes, améliorant la flexibilité, le suivi oculaire et l'amplitude des mouvements."
            ),
            FallbackExercise(
                title = "Changement de Focalisation",
                steps = "Placez votre pouce à 15 cm de votre nez. Concentrez-vous sur votre pouce, puis portez votre regard sur un objet lointain au bout de la pièce. Alternez toutes les 5 secondes.",
                durationSeconds = 40,
                benefit = "Renforce et étire le cristallin et les muscles d'accommodation, améliorant la souplesse de mise au point."
            ),
            FallbackExercise(
                title = "Rotation Circulaire des Yeux",
                steps = "Tenez-vous droit. Faites tourner lentement vos yeux en décrivant un cercle complet dans le sens des aiguilles d'une montre 5 fois, puis fermez-les 5 secondes. Répétez en sens inverse.",
                durationSeconds = 30,
                benefit = "Relâche la tension optique générale et étire les groupes musculaires oculaires restés immobiles face à l'écran."
            )
        ),
        "de" to listOf(
            FallbackExercise(
                title = "Die 20-20-20 Regel",
                steps = "Schauen Sie alle 20 Minuten vom Bildschirm weg. Fokussieren Sie für 20 Sekunden ein Objekt, das mindestens 20 Fuß (ca. 6 Meter) entfernt ist.",
                durationSeconds = 20,
                benefit = "Ermöglicht den Ziliarmuskeln im Auge eine vollständige Entspannung und mindert die Fokussierungsspannung durch Dauernahsehen."
            ),
            FallbackExercise(
                title = "Okulare Handauflegung (Palming)",
                steps = "Reiben Sie Ihre Handflächen energisch aneinander, bis sie warm werden. Legen Sie die warmen Hände sanft ohne Druck auf Ihre geschlossenen Augen und atmen Sie tief ein.",
                durationSeconds = 60,
                benefit = "Die Kombination aus Dunkelheit und wohltuender Wärme entspannt die Sehnerven, fördert die Tränenbildung und lindert visuelle Müdigkeit."
            ),
            FallbackExercise(
                title = "Bewusstes schnelles Blinzeln",
                steps = "Blinzeln Sie 10 bis 15 Mal schnell und sanft innerhalb von 20 Sekunden. Schließen Sie dann die Augen für 10 Sekunden. Wiederholen Sie das Ganze zweimal.",
                durationSeconds = 30,
                benefit = "Erneuert den Tränenfilm, befeuchtet trockene Augen und verhindert Augenreizungen, die durch vermindertes Blinzeln am Bildschirm entstehen."
            ),
            FallbackExercise(
                title = "Visuelle Acht (Figur 8)",
                steps = "Stellen Sie sich eine große, liegende Acht in 3 Metern Entfernung vor. Fahren Sie diese Form langsam 30 Sekunden lang mit Ihren Augen ab und wechseln Sie dann die Richtung.",
                durationSeconds = 60,
                benefit = "Trainiert die äußeren Augenmuskeln, erhält die Flexibilität des Augapfels und steigert das Koordinationsvermögen der Augen."
            ),
            FallbackExercise(
                title = "Fokuswechsel-Übung",
                steps = "Halten Sie Ihren Daumen ca. 15 cm vor die Nase. Fokussieren Sie den Daumen, blicken Sie dann auf ein weit entferntes Objekt im Raum. Wechseln Sie alle 5 Sekunden.",
                durationSeconds = 40,
                benefit = "Dehnt die Augenlinse und stärkt die Akkommodationsmuskulatur, was die allgemeine Schärfeeinstellung verbessert."
            ),
            FallbackExercise(
                title = "Sanftes Augenkreisen",
                steps = "Sitzen Sie aufrecht. Kreisen Sie Ihre Augen langsam 5-mal im Uhrzeigersinn, schließen Sie sie für 5 Sekunden. Wiederholen Sie den Vorgang gegen den Uhrzeigersinn.",
                durationSeconds = 30,
                benefit = "Baut aufgestaute Spannungen im Augenhintergrund ab, fördert die Durchblutung und lockert starre Blickhaltungen."
            )
        ),
        "es" to listOf(
            FallbackExercise(
                title = "La Regla 20-20-20",
                steps = "Cada 20 minutos, aparte la mirada de la pantalla. Enfoque sus ojos en un objeto a una distancia de al menos 20 pies (6 metros) durante 20 segundos.",
                durationSeconds = 20,
                benefit = "Permite que los músculos ciliares dentro de sus ojos se relajen por completo, reduciendo la fatiga de enfoque prolongado."
            ),
            FallbackExercise(
                title = "Palmeo Ocular (Palming)",
                steps = "Frote sus manos vigorosamente para generar un calor suave. Coloque las palmas cálidas sobre sus ojos cerrados sin presionar, y respire profundamente.",
                durationSeconds = 60,
                benefit = "La oscuridad total combinada con el calor reconfortante relaja las vías ópticas fatigadas, lubrica el ojo y alivia el cansancio muscular."
            ),
            FallbackExercise(
                title = "Parpadeo Rápido y Consciente",
                steps = "Parpadee de forma suave y rápida de 10 a 15 veces durante 20 segundos. Luego cierre los ojos y relájese durante otros 10 segundos. Repita dos veces.",
                durationSeconds = 30,
                benefit = "Restaura la película lagrimal protectora, lubrica la superficie corneal y evita la sequedad y enrojecimiento al mirar pantallas."
            ),
            FallbackExercise(
                title = "El 8 Ocular Imaginario",
                steps = "Imagine un número 8 grande acostado en el suelo a unos 3 metros de usted. Recórralo lentamente con la mirada por 30 segundos, y cambie el sentido.",
                durationSeconds = 60,
                benefit = "Ejercita y flexibiliza los músculos extraoculares externos de la órbita, mejorando el rango de movimiento coordinado."
            ),
            FallbackExercise(
                title = "Alternancia de Enfoques",
                steps = "Extienda su pulgar a unos 15 cm de su nariz. Mire fijamente el pulgar, luego cambie la mirada a un objeto lejano de la habitación. Intercale cada 5 segundos.",
                durationSeconds = 40,
                benefit = "Desarrolla fuerza y adaptabilidad en el cristalino y los músculos ciliares encargados del enfoque de profundidad."
            ),
            FallbackExercise(
                title = "Rotación de la Mirada",
                steps = "Siéntese erguido. Gire lentamente los ojos en círculo en el sentido de las agujas del reloj 5 veces, cierre los ojos por 5 segundos. Repita al revés.",
                durationSeconds = 30,
                benefit = "Alivia la tensión acumulada de fijar un único punto y estira suavemente los haces de soporte ocular."
            )
        )
    )

    // Safely retrieves a random exercise representing the scientifically backed backup catalog
    fun getRandomFallback(langCode: String): FallbackExercise {
        val normalizedCode = langCode.trim().lowercase()
        val list = exercisesByLanguage[normalizedCode] ?: exercisesByLanguage["en"]!!
        return list.random()
    }
}
