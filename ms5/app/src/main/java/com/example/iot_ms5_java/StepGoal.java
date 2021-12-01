package com.example.iot_ms5_java;

public class StepGoal {
    public int currSteps;
    public int stepGoalToday;
    public int stepGoalTomorrow;

    public StepGoal(int currSteps, int stepGoalToday, int stepGoalTomorrow) {
       this.currSteps = currSteps;
       this.stepGoalToday = stepGoalToday;
       this.stepGoalTomorrow = stepGoalTomorrow;
    }
}
