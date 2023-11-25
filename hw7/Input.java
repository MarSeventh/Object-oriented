import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.ElevatorRequest;
import com.oocourse.elevator3.MaintainRequest;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;

import java.io.IOException;
import java.util.ArrayList;

public class Input extends Thread {
    private final WaitList waitmap;
    private final Manager manager;
    private final ArrayList<Elevator> elevators;
    private final CountController countController;

    public Input(WaitList waitmap, Manager manager, CountController countController) {
        this.waitmap = waitmap;
        this.manager = manager;
        this.elevators = new ArrayList<>();
        this.countController = countController;
    }

    public void addElevators(Elevator elevator) {
        elevators.add(elevator);
    }

    @Override
    public void run() {
        ElevatorInput elevatorInput = new ElevatorInput(System.in);
        while (true) {
            Request request = elevatorInput.nextRequest();
            if (request == null) {
                try {
                    elevatorInput.close();
                    synchronized (waitmap) {
                        waitmap.notifyAll();
                    }
                    manager.setEnd(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            } else {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int id = personRequest.getPersonId();
                    int start = personRequest.getFromFloor();
                    int des = personRequest.getToFloor();
                    Person person = new Person(id, start, des);
                    waitmap.addPerson(person);
                } else if (request instanceof ElevatorRequest) {
                    ElevatorRequest elevatorRequest = (ElevatorRequest) request;
                    int id = elevatorRequest.getElevatorId();
                    int startpos = elevatorRequest.getFloor();
                    int capacity = elevatorRequest.getCapacity();
                    double speed = elevatorRequest.getSpeed();
                    int access = elevatorRequest.getAccess();
                    Elevator elevator = new Elevator(id, startpos, capacity,
                            speed, access, manager, waitmap, countController);
                    addElevators(elevator);
                    manager.addElevators(elevator);
                    elevator.start();
                } else if (request instanceof MaintainRequest) {
                    MaintainRequest maintainRequest = (MaintainRequest) request;
                    int id = maintainRequest.getElevatorId();
                    for (Elevator elevator : elevators) {
                        if (elevator.getID() == id) {
                            WaitList waitList = elevator.getWaitList();
                            synchronized (waitList) {
                                elevator.setMataining(true);
                                waitList.notifyAll();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}