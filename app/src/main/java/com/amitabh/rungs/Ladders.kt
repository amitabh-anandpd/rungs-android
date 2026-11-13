package com.amitabh.rungs

import com.amitabh.rungs.Measure.SECONDS

// Default ladders. Seeded once; editable in the app after that. Within a day the most valuable
// exercises come first: combined days (Upper, Full body) take only the first few of each.
// Never reorder or remove the levels of a shipped ladder: saved progress and sets point at them by index.
private fun reps(name: String, cue: String) = Level(name, cue, 8, 15)
private fun highReps(name: String, cue: String) = Level(name, cue, 10, 20)
private fun hold(name: String, cue: String) = Level(name, cue, 20, 45, SECONDS)

val DefaultLadders = listOf(
    Movement(
        "pushup", "Push-up", Day.PUSH, listOf(
            reps("Incline push-up", "Hands on the bed edge, body straight, chest to the edge."),
            reps("Knee push-up", "Knees down, hips in line, chest to the floor."),
            reps("Push-up", "Body in one straight line, elbows about 45° from the body."),
            reps("Diamond push-up", "Hands together under the chest, elbows close to the ribs."),
            reps("Decline push-up", "Feet on the chair, body in a straight line."),
            reps("Archer push-up", "Wide hands, lower towards one side, other arm straight. Reps per side."),
            reps("Pseudo-planche push-up", "Hands by the hips, fingers back, lean forward over them."),
            reps("Backpack push-up", "Push-up with the loaded backpack on your back."),
        ), startLevel = 4
    ),
    Movement(
        "pike", "Pike push-up", Day.PUSH, listOf(
            reps("Pike push-up", "Hips high, lower the top of the head towards the floor."),
            reps("Pike push-up, feet on bed", "Feet on the bed edge, hips over the shoulders."),
            reps("Pike push-up, feet on chair", "Feet on the chair, torso close to vertical."),
            hold("Wall handstand hold", "Chest to the wall, arms locked, body tight."),
        ), startLevel = 0
    ),
    Movement(
        "dip", "Dip", Day.PUSH, listOf(
            reps("Bed-edge dip, knees bent", "Hands on the bed edge, lower until elbows reach 90°."),
            reps("Bed-edge dip, legs straight", "Legs straight, heels on the floor, shoulders down."),
            reps("Bed-edge dip, feet on chair", "Feet on the chair, lower slowly to 90°."),
            reps("Dip, backpack on lap", "Feet on the chair with the loaded backpack on your thighs."),
        ), startLevel = 1
    ),
    Movement(
        "lateral", "Lateral raise", Day.PUSH, listOf(
            highReps("Backpack lateral raise", "Hold the backpack by its top handle, raise the arm out sideways to shoulder height, lower slowly. Reps per side."),
            highReps("Lean-away lateral raise", "Hold the door frame, lean away from it, raise the backpack out to shoulder height. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "triceps", "Triceps extension", Day.PUSH, listOf(
            reps("Bed-edge triceps extension", "Hands on the bed edge, bend only the elbows until your head dips below your hands, press back up."),
            reps("Floor triceps extension", "From a forearm plank, straighten the elbows to press up onto your hands, lower back to the forearms."),
            reps("Backpack overhead extension", "Hold the backpack behind your head with both hands, elbows pointing up, straighten the arms."),
        ), startLevel = 0
    ),
    Movement(
        "ohp", "Overhead press", Day.PUSH, listOf(
            reps("Backpack overhead press", "Stand tall, backpack at the chest, press it straight overhead, ribs down."),
            reps("One-arm backpack press", "Backpack at one shoulder, press overhead without leaning. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "bprow", "Backpack row", Day.PULL, listOf(
            reps("One-arm backpack row", "Hand and knee on the bed, pull the backpack to the hip. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "pullover", "Pullover", Day.PULL, listOf(
            reps("Backpack pullover", "Lie on the bed, head at the edge, arms nearly straight. Lower the backpack behind your head, pull it back over the chest."),
        ), startLevel = 0
    ),
    Movement(
        "bpcurl", "Backpack curl", Day.PULL, listOf(
            reps("Backpack curl", "Hold the backpack by the straps, elbows pinned to your sides."),
        ), startLevel = 0
    ),
    Movement(
        "reardelt", "Rear shoulder raise", Day.PULL, listOf(
            highReps("Prone Y raise", "Lie face down, arms in a Y, thumbs up. Lift the arms, squeeze the shoulder blades, pause 1 s."),
            highReps("Reverse snow angel", "Face down, arms held off the floor, sweep them from overhead down to the hips and back."),
            highReps("Bent-over backpack raise", "Hinge forward, one hand on the bed, raise the backpack out to the side with a soft elbow. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "bentrow", "Bent-over row", Day.PULL, listOf(
            reps("Bent-over backpack row", "Hinge to about 45° with a flat back, pull the backpack to the belly button with both hands, pause, lower slowly."),
        ), startLevel = 0
    ),
    Movement(
        "floorpull", "Floor pull", Day.PULL, listOf(
            reps("Elbow floor press", "Lie on your back, elbows bent beside the ribs. Drive the elbows into the floor to lift the upper back, pause at the top."),
            reps("Sliding floor pull", "Face down on a smooth floor, towel under the hips, arms reaching forward. Press the palms down and drag your body up to your hands."),
        ), startLevel = 0
    ),
    Movement(
        "doorrow", "Door row", Day.PULL, listOf(
            reps("Doorframe row", "Stand in a doorway, grip the frame at chest height, feet close to it. Lean back until the arms are straight, pull your chest to the frame."),
            reps("One-arm doorframe row", "As the doorframe row with one hand, hips square. Reps per side."),
            reps("Towel door row", "Only on a solid door: loop a towel round both handles, feet either side of the open door, lean back and row."),
        ), startLevel = 0
    ),
    Movement(
        "shrug", "Shrug", Day.PULL, listOf(
            highReps("Backpack shrug", "Hold the backpack in front with straight arms, lift the shoulders to the ears, pause, lower slowly."),
        ), startLevel = 0
    ),
    Movement(
        "tablerow", "Table row", Day.PULL, listOf(
            reps("Table row, knees bent", "Lie under the table, grip the edge, pull your chest up to it."),
            reps("Table row, legs straight", "Body straight from heels to head, chest to the edge."),
            reps("Table row, feet on chair", "Feet raised on the chair, body straight."),
            reps("Archer table row", "Pull towards one hand, other arm straight. Reps per side."),
            reps("One-arm table row", "One hand on the edge, don't let the hips rotate. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "squat", "Squat", Day.LEGS, listOf(
            highReps("Bodyweight squat", "Feet shoulder-width, sit back and down, knees track the toes."),
            highReps("Split squat", "Long stance, back knee lowers towards the floor. Reps per side."),
            highReps("Bulgarian split squat", "Rear foot on the bed, front shin roughly vertical. Reps per side."),
            highReps("Backpack Bulgarian split squat", "As Bulgarian, wearing the loaded backpack. Reps per side."),
            highReps("Pistol to chair", "One leg, sit down to the chair under control. Reps per side."),
        ), startLevel = 2
    ),
    Movement(
        "hip", "Hip", Day.LEGS, listOf(
            highReps("Glute bridge", "Feet flat, squeeze the glutes and lift the hips to a straight line."),
            highReps("Single-leg bridge", "One foot down, other leg up, hips stay level. Reps per side."),
            highReps("Hip thrust", "Shoulders on the bed edge, drive the hips up, pause at the top."),
            highReps("Single-leg hip thrust", "Shoulders on the bed, one foot down. Reps per side."),
            highReps("Backpack hip thrust", "Shoulders on the bed, loaded backpack on the hips."),
        ), startLevel = 1
    ),
    Movement(
        "legcurl", "Hamstring curl", Day.LEGS, listOf(
            reps("Sliding leg curl", "Lie on your back, heels on a towel on a smooth floor. Lift the hips and drag the heels in towards you."),
            reps("Single-leg sliding curl", "As the sliding curl with one heel, hips stay up. Reps per side."),
            Level("Nordic lowering", "Kneel on a pillow, feet hooked under the bed. Lower your body as slowly as you can, push back up with your hands.", 3, 8),
        ), startLevel = 0
    ),
    Movement(
        "calf", "Calf raise", Day.LEGS, listOf(
            highReps("Calf raise", "Both feet on the floor, rise high, lower slowly."),
            highReps("Single-leg calf raise", "Ball of the foot on a book stack, full stretch at the bottom. Reps per side."),
            highReps("Backpack single-leg calf raise", "As single-leg, wearing the loaded backpack. Reps per side."),
        ), startLevel = 1
    ),
    Movement(
        "core", "Core", Day.LEGS, listOf(
            hold("Plank", "Forearms down, body straight, squeeze glutes and abs."),
            hold("Hollow hold, knees tucked", "Lower back pressed down, knees over the hips."),
            hold("Hollow hold", "Lower back pressed down, arms and legs long and low."),
            highReps("Lying leg raise", "Lower back pressed down, lower the legs slowly."),
        ), startLevel = 1
    ),
    Movement(
        "hinge", "Hip hinge", Day.LEGS, listOf(
            highReps("Backpack good morning", "Hug the backpack to your chest, knees soft, push the hips back with a flat back until the hamstrings stretch."),
            highReps("Single-leg backpack deadlift", "Stand on one leg, hinge forward with a flat back, lower the backpack towards the floor. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "lunge", "Lunge", Day.LEGS, listOf(
            highReps("Reverse lunge", "Step back, lower the back knee to just above the floor, push through the front heel. Reps per side."),
            highReps("Walking lunge", "Long steps forward, torso upright, back knee to just above the floor. Reps per side."),
            highReps("Backpack reverse lunge", "As the reverse lunge, wearing the loaded backpack. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "wallsit", "Wall sit", Day.LEGS, listOf(
            hold("Wall sit", "Back flat on the wall, thighs parallel to the floor, hands off the legs."),
            hold("Single-leg wall sit", "As the wall sit with one foot off the floor. Time per side."),
        ), startLevel = 0
    ),
    Movement(
        "sideplank", "Side plank", Day.LEGS, listOf(
            hold("Side plank, knees down", "On one forearm and the knees, hips pushed forward and up. Time per side."),
            hold("Side plank", "On one forearm and the feet, body in one straight line. Time per side."),
            hold("Side plank, top leg raised", "Side plank with the top leg lifted. Time per side."),
        ), startLevel = 0
    ),
)

/**
 * The default plan: what Today shows until the user picks their own. No table rows and nothing
 * that needs a bar; everything else is one tap away under "More exercises".
 */
private val DefaultPlan = setOf(
    "pushup", "pike", "dip", "lateral",
    "bprow", "pullover", "bpcurl", "reardelt",
    "squat", "hip", "legcurl", "calf", "core",
)

/** A one-category day does the whole default plan for it; combined days take the first 3 or 2 per category. */
fun defaultPicks(split: Split): Set<String> = split.slots.flatMapTo(LinkedHashSet()) { slot ->
    val perDay = when (slot.size) { 1 -> Int.MAX_VALUE; 2 -> 3; else -> 2 }
    slot.flatMap { day -> DefaultLadders.filter { it.day == day && it.id in DefaultPlan }.take(perDay).map { it.id } }
}
