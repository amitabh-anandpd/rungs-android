package com.amitabh.rungs

import com.amitabh.rungs.Measure.SECONDS

// Default ladders from the product brief. Seeded once; editable in the app after that.
private fun reps(name: String, cue: String) = Level(name, cue, 8, 15)
private fun legReps(name: String, cue: String) = Level(name, cue, 10, 20)
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
        "tablerow", "Table row", Day.PULL, listOf(
            reps("Table row, knees bent", "Lie under the table, grip the edge, pull your chest up to it."),
            reps("Table row, legs straight", "Body straight from heels to head, chest to the edge."),
            reps("Table row, feet on chair", "Feet raised on the chair, body straight."),
            reps("Archer table row", "Pull towards one hand, other arm straight. Reps per side."),
            reps("One-arm table row", "One hand on the edge, don't let the hips rotate. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "bprow", "Backpack row", Day.PULL, listOf(
            reps("One-arm backpack row", "Hand and knee on the bed, pull the backpack to the hip. Reps per side."),
        ), startLevel = 0
    ),
    Movement(
        "bpcurl", "Backpack curl", Day.PULL, listOf(
            reps("Backpack curl", "Hold the backpack by the straps, elbows pinned to your sides."),
        ), startLevel = 0
    ),
    Movement(
        "squat", "Squat", Day.LEGS, listOf(
            legReps("Bodyweight squat", "Feet shoulder-width, sit back and down, knees track the toes."),
            legReps("Split squat", "Long stance, back knee lowers towards the floor. Reps per side."),
            legReps("Bulgarian split squat", "Rear foot on the bed, front shin roughly vertical. Reps per side."),
            legReps("Backpack Bulgarian split squat", "As Bulgarian, wearing the loaded backpack. Reps per side."),
            legReps("Pistol to chair", "One leg, sit down to the chair under control. Reps per side."),
        ), startLevel = 2
    ),
    Movement(
        "hip", "Hip", Day.LEGS, listOf(
            legReps("Glute bridge", "Feet flat, squeeze the glutes and lift the hips to a straight line."),
            legReps("Single-leg bridge", "One foot down, other leg up, hips stay level. Reps per side."),
            legReps("Hip thrust", "Shoulders on the bed edge, drive the hips up, pause at the top."),
            legReps("Single-leg hip thrust", "Shoulders on the bed, one foot down. Reps per side."),
            legReps("Backpack hip thrust", "Shoulders on the bed, loaded backpack on the hips."),
        ), startLevel = 1
    ),
    Movement(
        "calf", "Calf raise", Day.LEGS, listOf(
            legReps("Calf raise", "Both feet on the floor, rise high, lower slowly."),
            legReps("Single-leg calf raise", "Ball of the foot on a book stack, full stretch at the bottom. Reps per side."),
            legReps("Backpack single-leg calf raise", "As single-leg, wearing the loaded backpack. Reps per side."),
        ), startLevel = 1
    ),
    Movement(
        "core", "Core", Day.LEGS, listOf(
            hold("Plank", "Forearms down, body straight, squeeze glutes and abs."),
            hold("Hollow hold, knees tucked", "Lower back pressed down, knees over the hips."),
            hold("Hollow hold", "Lower back pressed down, arms and legs long and low."),
            legReps("Lying leg raise", "Lower back pressed down, lower the legs slowly."),
        ), startLevel = 1
    ),
)
