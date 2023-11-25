import com.oocourse.spec2.exceptions.AcquaintanceNotFoundException;
import com.oocourse.spec2.exceptions.EqualGroupIdException;
import com.oocourse.spec2.exceptions.EqualMessageIdException;
import com.oocourse.spec2.exceptions.EqualPersonIdException;
import com.oocourse.spec2.exceptions.EqualRelationException;
import com.oocourse.spec2.exceptions.GroupIdNotFoundException;
import com.oocourse.spec2.exceptions.MessageIdNotFoundException;
import com.oocourse.spec2.exceptions.PersonIdNotFoundException;
import com.oocourse.spec2.exceptions.RelationNotFoundException;
import com.oocourse.spec2.main.Group;
import com.oocourse.spec2.main.Message;
import com.oocourse.spec2.main.Network;
import com.oocourse.spec2.main.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyNetwork implements Network {
    private final HashMap<Integer, Person> people;
    private final HashMap<Integer, Group> groups;
    private final HashMap<Integer, Message> messages;
    private int qtsum;
    private int cpsum;
    private final ArrayList<HashMap<Integer, Person>> blocks;

    public MyNetwork() {
        this.people = new HashMap<>();
        this.groups = new HashMap<>();
        this.messages = new HashMap<>();
        this.qtsum = 0;
        this.cpsum = 0;
        this.blocks = new ArrayList<>();
    }

    public boolean contains(int id) {
        return people.containsKey(id);
    }

    public Person getPerson(int id) {
        return people.getOrDefault(id, null);
    }

    public void addPerson(Person person) throws EqualPersonIdException {
        if (contains(person.getId())) {
            throw new MyEqualPersonIdException(person.getId());
        } else {
            people.put(person.getId(), person);
            HashMap<Integer, Person> newblock = new HashMap<>();
            newblock.put(person.getId(), person);
            blocks.add(newblock);
        }
    }

    public void addRelation(int id1, int id2, int value) throws
            PersonIdNotFoundException, EqualRelationException {
        if (contains(id1) && contains(id2) && !getPerson(id1).isLinked(getPerson(id2))) {
            MyPerson myPerson1 = (MyPerson) getPerson(id1);
            MyPerson myPerson2 = (MyPerson) getPerson(id2);
            int oldcp1 = myPerson1.getBestAcquaintance();
            int oldcp2 = myPerson2.getBestAcquaintance();
            myPerson1.addAcq(myPerson2, value);
            myPerson2.addAcq(myPerson1, value);
            manageCouple(id1, id2, oldcp1, oldcp2);
            manageTriple(id1, id2);
            manageBlock(id1, id2);
            for (int id : groups.keySet()) {
                MyGroup group = (MyGroup) groups.get(id);
                if (group.hasPerson(myPerson1) && group.hasPerson(myPerson2)) {
                    group.manageValue(null, value, 2);
                }
            }
        } else {
            if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else if (!contains(id2)) {
                throw new MyPersonIdNotFoundException(id2);
            } else {
                throw new MyEqualRelationException(id1, id2);
            }
        }
    }

    public void modifyRelation(int id1, int id2, int value) throws PersonIdNotFoundException,
            EqualPersonIdException, RelationNotFoundException {
        if (contains(id1) && contains(id2) && id1 != id2
                && getPerson(id1).isLinked(getPerson(id2))) {
            MyPerson person1 = (MyPerson) getPerson(id1);
            MyPerson person2 = (MyPerson) getPerson(id2);
            int oldvalue = person1.queryValue(person2);
            int value1 = (oldvalue + value < 0) ? -oldvalue : value;
            int oldcp1 = person1.getBestAcquaintance();
            person1.modifyAcq(person2, value);
            int oldcp2 = person2.getBestAcquaintance();
            person2.modifyAcq(person1, value);
            manageCouple(id1, id2, oldcp1, oldcp2);
            for (int id : groups.keySet()) {
                MyGroup group = (MyGroup) groups.get(id);
                if (group.hasPerson(person1) && group.hasPerson(person2)) {
                    group.manageValue(null, value1, 2);
                }
            }
            if (oldvalue + value <= 0) {
                deleteBlock(id1, id2);
                deleteTriple(id1, id2);
            }
        } else {
            if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else if (!contains(id2)) {
                throw new MyPersonIdNotFoundException(id2);
            } else if (id1 == id2) {
                throw new MyEqualPersonIdException(id1);
            } else {
                throw new MyRelationNotFoundException(id1, id2);
            }
        }
    }

    public void manageCouple(int id1, int id2, int oldcp1, int oldcp2) {
        MyPerson person1 = (MyPerson) getPerson(id1);
        MyPerson person2 = (MyPerson) getPerson(id2);
        int newcp1 = person1.getBestAcquaintance();
        int newcp2 = person2.getBestAcquaintance();
        MyPerson new1 = (MyPerson) getPerson(newcp1);
        MyPerson new2 = (MyPerson) getPerson(newcp2);
        MyPerson old1 = (MyPerson) getPerson(oldcp1);
        MyPerson old2 = (MyPerson) getPerson(oldcp2);
        if (oldcp1 != newcp1 || oldcp2 != newcp2) {
            if (newcp1 == id2 && newcp2 == id1) {
                cpsum++;
            }
            if (oldcp1 == id2 && oldcp2 == id1) {
                cpsum--;
            }
            if (newcp1 != oldcp1) {
                if (newcp1 == id2) {
                    if (old1 != null && old1.getBestAcquaintance() == id1) {
                        cpsum--;
                    }
                } else {
                    if (new1 != null && new1.getBestAcquaintance() == id1) {
                        cpsum++;
                    }
                }
            }
            if (newcp2 != oldcp2) {
                if (newcp2 == id1) {
                    if (old2 != null && old2.getBestAcquaintance() == id2) {
                        cpsum--;
                    }
                } else {
                    if (new2 != null && new2.getBestAcquaintance() == id2) {
                        cpsum++;
                    }
                }
            }
        }
    }

    public void deleteBlock(int id1, int id2) {
        HashMap<Integer, Person> dblock = null;
        for (HashMap<Integer, Person> block : blocks) {
            if (block.containsKey(id1) && block.containsKey(id2)) {
                dblock = block;
                break;
            }
        }
        if (dblock != null) {
            HashMap<Integer, Person> newblock = new HashMap<>();
            getOneBlock(id1, dblock, newblock);
            if (!newblock.containsKey(id2)) {
                dblock.keySet().removeAll(newblock.keySet());
                blocks.add(newblock);
            }
        }
    }

    public void getOneBlock(int id, HashMap<Integer, Person> people,
                            HashMap<Integer, Person> newblock) {
        MyPerson myperson = (MyPerson) getPerson(id);
        newblock.put(id, getPerson(id));
        for (Person person : myperson.getAcqArray()) {
            if (people.containsKey(person.getId()) && !newblock.containsKey(person.getId())) {
                getOneBlock(person.getId(), people, newblock);
            }
        }
    }

    public void deleteTriple(int id1, int id2) {
        for (int id : people.keySet()) {
            if (id == id1 || id == id2) {
                continue;
            }
            if (getPerson(id1).isLinked(people.get(id)) &&
                    getPerson(id2).isLinked(people.get(id))) {
                qtsum--;
            }
        }
    }

    public void manageBlock(int id1, int id2) {
        HashMap<Integer, Person> block1 = new HashMap<>();
        for (HashMap<Integer, Person> block : blocks) {
            if (block.containsKey(id1)) {
                block1 = block;
                break;
            }
        }
        if (!block1.containsKey(id2)) {
            HashMap<Integer, Person> block2 = new HashMap<>();
            for (HashMap<Integer, Person> block : blocks) {
                if (block.containsKey(id2)) {
                    block2 = block;
                    blocks.remove(block);
                    break;
                }
            }
            block1.putAll(block2);
        }
    }

    public void manageTriple(int id1, int id2) {
        for (int id : people.keySet()) {
            if (id == id1 || id == id2) {
                continue;
            }
            if (people.get(id).isLinked(getPerson(id1))
                    && people.get(id).isLinked(getPerson(id2))) {
                qtsum++;
            }
        }
    }

    public int queryValue(int id1, int id2) throws
            PersonIdNotFoundException, RelationNotFoundException {
        if (contains(id1) && contains(id2) && getPerson(id1).isLinked(getPerson(id2))) {
            return getPerson(id1).queryValue(getPerson(id2));
        } else {
            if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else if (!contains(id2)) {
                throw new MyPersonIdNotFoundException(id2);
            } else {
                throw new MyRelationNotFoundException(id1, id2);
            }
        }
    }

    public boolean isCircle(int id1, int id2) throws PersonIdNotFoundException {
        if (contains(id1) && contains(id2)) {
            for (HashMap<Integer, Person> block : blocks) {
                if (block.containsKey(id1) && block.containsKey(id2)) {
                    return true;
                }
            }
            return false;
        } else {
            if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else {
                throw new MyPersonIdNotFoundException(id2);
            }
        }
    }

    public int queryBlockSum() {
        return blocks.size();
    }

    public int queryTripleSum() {
        return qtsum;
    }

    public void addGroup(Group group) throws EqualGroupIdException {
        if (!groups.containsKey(group.getId())) {
            groups.put(group.getId(), group);
        } else {
            throw new MyEqualGroupIdException(group.getId());
        }
    }

    public Group getGroup(int id) {
        return groups.getOrDefault(id, null);
    }

    public void addToGroup(int id1, int id2) throws GroupIdNotFoundException,
            PersonIdNotFoundException, EqualPersonIdException {
        if (people.containsKey(id1) && groups.containsKey(id2) &&
                !getGroup(id2).hasPerson(getPerson(id1))) {
            if (getGroup(id2).getSize() <= 1111) {
                getGroup(id2).addPerson(getPerson(id1));
            }
        } else {
            if (!groups.containsKey(id2)) {
                throw new MyGroupIdNotFoundException(id2);
            } else if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else if (getGroup(id2).hasPerson(getPerson(id1))) {
                throw new MyEqualPersonIdException(id1);
            }
        }
    }

    public int queryGroupValueSum(int id) throws GroupIdNotFoundException {
        if (groups.containsKey(id)) {
            return getGroup(id).getValueSum();
        } else {
            throw new MyGroupIdNotFoundException(id);
        }
    }

    public int queryGroupAgeVar(int id) throws GroupIdNotFoundException {
        if (groups.containsKey(id)) {
            return getGroup(id).getAgeVar();
        } else {
            throw new MyGroupIdNotFoundException(id);
        }
    }

    public void delFromGroup(int id1, int id2)
            throws GroupIdNotFoundException, PersonIdNotFoundException, EqualPersonIdException {
        if (getGroup(id2) != null && contains(id1) && getGroup(id2).hasPerson(getPerson(id1))) {
            getGroup(id2).delPerson(getPerson(id1));
        } else {
            if (!groups.containsKey(id2)) {
                throw new MyGroupIdNotFoundException(id2);
            } else if (!contains(id1)) {
                throw new MyPersonIdNotFoundException(id1);
            } else if (!getGroup(id2).hasPerson(getPerson(id1))) {
                throw new MyEqualPersonIdException(id1);
            }
        }
    }

    public boolean containsMessage(int id) {
        return messages.containsKey(id);
    }

    public void addMessage(Message message) throws
            EqualMessageIdException, EqualPersonIdException {
        if (!messages.containsKey(message.getId()) &&
                (message.getType() == 1 ||
                        (message.getType() == 0 &&
                                !message.getPerson1().equals(message.getPerson2())))) {
            messages.put(message.getId(), message);
        } else {
            if (messages.containsKey(message.getId())) {
                throw new MyEqualMessageIdException(message.getId());
            } else if (message.getType() == 0 &&
                    message.getPerson1().equals(message.getPerson2())) {
                throw new MyEqualPersonIdException(message.getPerson1().getId());
            }
        }
    }

    public Message getMessage(int id) {
        return messages.getOrDefault(id, null);
    }

    public void sendMessage(int id) throws
            RelationNotFoundException, MessageIdNotFoundException, PersonIdNotFoundException {
        Message message = getMessage(id);
        if (containsMessage(id) && message.getType() == 0
                && message.getPerson1().isLinked(message.getPerson2()) &&
                !message.getPerson1().equals(message.getPerson2())) {
            Person person1 = message.getPerson1();
            Person person2 = message.getPerson2();
            person1.addSocialValue(message.getSocialValue());
            person2.addSocialValue(message.getSocialValue());
            messages.remove(id);
            person2.getMessages().add(0, message);
        } else if (containsMessage(id) && message.getType() == 1 &&
                message.getGroup().hasPerson(message.getPerson1())) {
            MyGroup mygroup = (MyGroup) message.getGroup();
            mygroup.addSocialValue(message.getSocialValue());
            messages.remove(id);
        } else {
            if (!containsMessage(id)) {
                throw new MyMessageIdNotFoundException(id);
            } else {
                if (message.getType() == 0 &&
                        !message.getPerson1().isLinked(message.getPerson2())) {
                    throw new MyRelationNotFoundException(message.getPerson1().getId(),
                            message.getPerson2().getId());
                }
                if (message.getType() == 1 &&
                        !message.getGroup().hasPerson(message.getPerson1())) {
                    throw new MyPersonIdNotFoundException(message.getPerson1().getId());
                }
            }
        }
    }

    public int querySocialValue(int id) throws PersonIdNotFoundException {
        if (contains(id)) {
            return getPerson(id).getSocialValue();
        } else {
            throw new MyPersonIdNotFoundException(id);
        }
    }

    public List<Message> queryReceivedMessages(int id) throws PersonIdNotFoundException {
        if (contains(id)) {
            return getPerson(id).getReceivedMessages();
        } else {
            throw new MyPersonIdNotFoundException(id);
        }
    }

    public int queryBestAcquaintance(int id) throws
            PersonIdNotFoundException, AcquaintanceNotFoundException {
        if (contains(id)) {
            MyPerson myperson = (MyPerson) getPerson(id);
            int bestid = myperson.getBestAcquaintance();
            if (bestid != -1) {
                return bestid;
            } else {
                throw new MyAcquaintanceNotFoundException(id);
            }
        } else {
            throw new MyPersonIdNotFoundException(id);
        }
    }

    public int queryCoupleSum() {
        return cpsum;
    }

    public int modifyRelationOKTest(int id1, int id2, int value,
                                    HashMap<Integer, HashMap<Integer, Integer>> beforeData,
                                    HashMap<Integer, HashMap<Integer, Integer>> afterData) {
        ModifyRelationOkTest test = new ModifyRelationOkTest();
        return test.test(id1, id2, value, beforeData, afterData);
    }
}