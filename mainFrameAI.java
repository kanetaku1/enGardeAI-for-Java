/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package enGardeAI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import engarde.gui.EnGardeGUI;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author ktajima
 */
public class mainFrameAI extends javax.swing.JFrame implements ActionListener{

    private String serverAddress;
    private int serverPort;

    //Socket版    
    private Socket connectedSocket = null;
    private BufferedReader serverReader;
    private MessageReceiver receiver;
    private PrintWriter serverWriter;
    
    //表示部分のドキュメントを管理するクラス
    private DefaultStyledDocument document_server;
    private DefaultStyledDocument document_system;

    //AI
    private int p0position = 1;
    private int p1position = 23;
    private int distance = 0;
    private int cardNumber = 0;
    private String action = "";
    private int currentPlayer = -1;

    /** HashMapを送るメソッド */
    private void sendMassageWithSocket(HashMap<String,String> data) throws IOException, InterruptedException{
            StringBuilder response = new StringBuilder();
            ObjectMapper mapper = new ObjectMapper();
            //response.append("<json>");
            response.append(mapper.writeValueAsString(data));
            //response.append("</json>");
            this.serverWriter.println(response.toString());
            this.serverWriter.flush();
            //DEBUG
            this.printMessage("[Sent]"+data.toString());
            //DEBUG
    }

    /** 指定した文字列を送るメソッド */
    private void sendMassageWithSocket(String message) throws IOException, InterruptedException{
            this.serverWriter.println(message);
            this.serverWriter.flush();
            //DEBUG
            this.printMessage("[Sent]"+message);
            //DEBUG
    }

    
    private void connectToServer(){
        this.serverAddress = this.jTextField1.getText();
        this.serverPort = Integer.parseInt(this.jTextField2.getText());

        // Socket版
        try {
            this.connectedSocket = new Socket(this.serverAddress,this.serverPort);
            this.serverReader = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()));
            this.serverWriter = new PrintWriter(new OutputStreamWriter(connectedSocket.getOutputStream()));
        
            this.receiver = new MessageReceiver(this);
            this.receiver.start();
            this.printMessage("サーバに接続しました。");
            
        } catch (IOException ex) {
            this.printMessage("サーバに接続できませんでした。");
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /** スレッドに読み込みを行わせる用の取り出しメソッド
     * @return  */
    public BufferedReader getServerReader(){
        return this.serverReader;
    }
    
    /** スレッドから読みこんだメッセージを受信するメソッド
     * @param message */
    public void receiveMessageFromServer(String message){
        this.showRecivedMessage(message);
        try {
            //JSON -> HashMAP
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String,String> de_map = mapper.readValue(message, HashMap.class);
            this.receiveDataFromServer(de_map);

        } catch (JsonProcessingException ex) {
        }
    }
    
    /** GUI上に受信したメッセージを表示するメソッド
     * @param message */
    public void showRecivedMessage(String message){
        try {
            SimpleAttributeSet attribute = new SimpleAttributeSet();
            attribute.addAttribute(StyleConstants.Foreground, Color.BLACK);
            //ドキュメントにその属性情報つきの文字列を挿入
            document_server.insertString(document_server.getLength(), message+"\n", attribute);
            this.jTextArea1.setCaretPosition(document_server.getLength());

        } catch (BadLocationException ex) {
        }
    }
    
    /** GUI上にシステムメッセージを表示するメソッド
     * @param message */
    public void printMessage(String message){
        System.out.println(message);
        try {
            SimpleAttributeSet attribute = new SimpleAttributeSet();
            attribute.addAttribute(StyleConstants.Foreground, Color.BLACK);
            //ドキュメントにその属性情報つきの文字列を挿入
            document_system.insertString(document_system.getLength(), message+"\n", attribute);
            this.jTextArea3.setCaretPosition(document_system.getLength());

        } catch (BadLocationException ex) {
        }
    }

    private int myPlayerID = -1;
    
    /** データハンドリングメソッド
     * @param data */
    public void receiveDataFromServer(HashMap<String,String> data){
        if(!data.containsKey("Type")){
            return;
        }
        try {
            String type = data.get("Type");
            switch(type){
                case "ConnectionStart":
                    //自分の番号の確定
                    this.myPlayerID = Integer.parseInt(data.get("ClientID"));
                    //なまえの入力
                    this.sendMyName();
                    this.view = new EnGardeGUI();
                    this.view.addEventListener(this);
                    break;
                case "NameReceived":
                    break;
                case "BoardInfo":
                    this.showBoard(data);
                    break;
                case "HandInfo":
                    this.showHand(data);
                    break;
                case "DoPlay":
                    this.DoPlay();
                    break;
                default:
            }
        } catch (IOException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private EnGardeGUI view;
    private ArrayList<Integer> myCardList = new ArrayList<>();

    /** 現在のボードの描画 */
    private void showBoard(HashMap<String,String> data) {
        //{"Type":"BoardInfo","PlayerPosition_1":"23","NumofDeck":"15","CurrentPlayer":"0","PlayerPosition_0":"1","From":"Server","To":"Client","PlayerScore_0":"0","PlayerScore_1":"0"}
        String currentPlayerStr = data.get("CurrentPlayer");
        if (currentPlayerStr != null) {
            this.currentPlayer = Integer.parseInt(currentPlayerStr);
            this.printMessage("currentPlayer = " + currentPlayer);
        }
        // int currentPlayer = Integer.parseInt(data.get("CurrentPlayer"));
        int player0Score = Integer.parseInt(data.get("PlayerScore_0"));
        int player1Score = Integer.parseInt(data.get("PlayerScore_1"));
        this.p0position = Integer.parseInt(data.get("PlayerPosition_0"));
        this.p1position = Integer.parseInt(data.get("PlayerPosition_1"));
        int DeckCount = Integer.parseInt(data.get("NumofDeck"));
        //距離を求める
        this.distance = Math.abs(p0position - p1position);
        this.printMessage("GamePlayerNumber = " + this.myPlayerID);
        this.view.setDrawData(currentPlayer, player0Score, player1Score, p0position, p1position, DeckCount, new ArrayList<Integer>(), new ArrayList<Integer>());
        this.view.setPlayerHand(myPlayerID, myCardList);
        this.view.setVisible(true);
    }
    /** 手札の状況 */
    private void showHand(HashMap<String,String> data) {
        //{Hand3=1, Hand4=3, Type=HandInfo, Hand5=1, Hand1=5, Hand2=4, From=Server, To=Client}
        this.myCardList.clear();
        String[] keys = {"Hand1","Hand2","Hand3","Hand4","Hand5"};
        for(String key:keys){
            if(data.containsKey(key)){
                this.myCardList.add(Integer.parseInt(data.get(key)));
            }
        }
        this.view.setPlayerHand(myPlayerID, myCardList);
        try {
            this.sendEvaluateMessage();
        } catch (IOException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.view.setVisible(true);
    }
    private void sendMyName() throws IOException, InterruptedException{
        String inputValue = "EnGardeAI";
        // JOptionPane.showInputDialog("Please your player name");
        //JSON構成
        //likes <json>{"Type":"PlayerName","From":"Client","To":"Server","Name":"Simple"}</json>
        HashMap<String, String> response = new HashMap<>();
        response.put("From","Client");
        response.put("To","Server");
        response.put("Type","PlayerName");
        response.put("Name",inputValue);
        StringBuilder sbuf = new StringBuilder();
        try {
            ObjectMapper mapper = new ObjectMapper();
            //sbuf.append("<json>");
            sbuf.append(mapper.writeValueAsString(response));
            //sbuf.append("</json>");
        } catch (JsonProcessingException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sbuf.length() > 0){
            this.sendMassageWithSocket(sbuf.toString());
        }
        
    }
    private void sendForwardMessage(int cardNumber) throws IOException, InterruptedException{
        if(!this.myCardList.contains(cardNumber)){
            return;
        }
        //JSON構成
        //likes <json>{"Type":"PlayerName","From":"Client","To":"Server","Name":"Simple"}</json>
        HashMap<String, String> response = new HashMap<>();
        response.put("From","Client");
        response.put("To","Server");
        response.put("Type","Play");
        response.put("Direction","F");
        response.put("MessageID","101");
        response.put("PlayCard",Integer.toString(cardNumber));
        
        StringBuilder sbuf = new StringBuilder();
        try {
            ObjectMapper mapper = new ObjectMapper();
            //sbuf.append("<json>");
            sbuf.append(mapper.writeValueAsString(response));
            //sbuf.append("</json>");
        } catch (JsonProcessingException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sbuf.length() > 0){
            this.sendMassageWithSocket(sbuf.toString());
        }
    }


    private void sendBackwardMessage(int cardNumber) throws IOException, InterruptedException{
        if(!this.myCardList.contains(cardNumber)){
            return;
        }
        //JSON構成
        //likes <json>{"Type":"PlayerName","From":"Client","To":"Server","Name":"Simple"}</json>
        HashMap<String, String> response = new HashMap<>();
        response.put("From","Client");
        response.put("To","Server");
        response.put("Type","Play");
        response.put("Direction","B");
        response.put("MessageID","101");
        response.put("PlayCard",Integer.toString(cardNumber));
        
        StringBuilder sbuf = new StringBuilder();
        try {
            ObjectMapper mapper = new ObjectMapper();
            //sbuf.append("<json>");
            sbuf.append(mapper.writeValueAsString(response));
            //sbuf.append("</json>");
        } catch (JsonProcessingException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sbuf.length() > 0){
            this.sendMassageWithSocket(sbuf.toString());
        }
    }
    
    private void sendAttackMessage(int cardNumber,int cardCount) throws IOException, InterruptedException{
        if(!this.myCardList.contains(cardNumber)){
            return;
        }
        //JSON構成
        //<json>{"Type":"Play","PlayCard":"4","From":"Client","To":"Server","NumOfCard":"2","MessageID":"102"}</json>
        HashMap<String, String> response = new HashMap<>();
        response.put("From","Client");
        response.put("To","Server");
        response.put("Type","Play");
        response.put("NumOfCard","B");
        response.put("MessageID","102");
        response.put("NumOfCard",Integer.toString(cardCount));
        response.put("PlayCard",Integer.toString(cardNumber));
        
        StringBuilder sbuf = new StringBuilder();
        try {
            ObjectMapper mapper = new ObjectMapper();
            //sbuf.append("<json>");
            sbuf.append(mapper.writeValueAsString(response));
            //sbuf.append("</json>");
        } catch (JsonProcessingException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sbuf.length() > 0){
            this.sendMassageWithSocket(sbuf.toString());
        }
    }
    
    /** このメソッドは単に固定値を送るだけ */
     private void sendEvaluateMessage() throws IOException, InterruptedException{
        //JSON構成
        //<json>{"Type":"Evaluation","PlayCard":"4","From":"Client","To":"Server","NumOfCard":"2","MessageID":"102"}</json>
        HashMap<String, String> response = new HashMap<>();
        response.put("From","Client");
        response.put("To","Server");
        response.put("Type","Evaluation");
        response.put("1F","0.1");
        response.put("2F","0.1");
        response.put("3F","0.1");
        response.put("4F","0.1");
        response.put("5F","0.1");
        response.put("1B","0.1");
        response.put("2B","0.1");
        response.put("3B","0.1");
        response.put("4B","0.1");
        response.put("5B","0.1");
        
        StringBuilder sbuf = new StringBuilder();
        try {
            ObjectMapper mapper = new ObjectMapper();
            //sbuf.append("<json>");
            sbuf.append(mapper.writeValueAsString(response));
            //sbuf.append("</json>");
        } catch (JsonProcessingException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sbuf.length() > 0){
            this.sendMassageWithSocket(sbuf.toString());
        }
    }   
    
    /**
     * Creates new form mainFrame
     */
    public mainFrameAI() {
        initComponents();
        this.document_server = new DefaultStyledDocument();
        this.document_system = new DefaultStyledDocument();
        
        this.jTextArea1.setDocument(this.document_server);
        this.jTextArea3.setDocument(this.document_system);
    }
        

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("2024 情報工学実験 En Garde GUI Sever 1.0");

        jTextField1.setText("localhost");

        jLabel2.setText("ServerIP");

        jLabel3.setText("Port");

        jTextField2.setText("12052");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jButton1.setText("Connect");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Recived");

        jLabel5.setText("Send JSON Data");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jButton2.setText("Send");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Make");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel6.setText("SystemMessage");

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jScrollPane3.setViewportView(jTextArea3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addGap(0, 327, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextField1)
                                .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE))))
                    .addComponent(jLabel5)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jButton1)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 366, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel6)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jButton3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2))
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jButton1))
                .addGap(4, 4, 4)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        JsonMakerAI dialog = new JsonMakerAI(this, false);
        dialog.setVisible(true);
        dialog.setMainFrame(this);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        this.connectToServer();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            this.sendMassageWithSocket(this.jTextArea2.getText());
        } catch (IOException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.jTextArea2.setText("");
    }//GEN-LAST:event_jButton2ActionPerformed

    /** サブウインドウからの変更要求 */
    public void setSendText(String message){
        this.jTextArea2.setText(message);
    }
    
    private int getCardID(String command){
        int cardID = -1;
        if(command.equals("Player0_Card1")){
            cardID = 0;
        } else if(command.equals("Player0_Card2")){
            cardID = 1;
        } else if(command.equals("Player0_Card3")){
            cardID = 2;
        } else if(command.equals("Player0_Card4")){
            cardID = 3;
        } else if(command.equals("Player0_Card5")){
            cardID = 4;
        }
        if(command.equals("Player1_Card1")){
            cardID = 0;
        } else if(command.equals("Player1_Card2")){
            cardID = 1;
        } else if(command.equals("Player1_Card3")){
            cardID = 2;
        } else if(command.equals("Player1_Card4")){
            cardID = 3;
        } else if(command.equals("Player1_Card5")){
            cardID = 4;
        }
        return cardID;
    }
    
    /** GUIからのrequest処理 */
    @Override
    public void actionPerformed(ActionEvent e) {
        int type_id = e.getID();
        String command = e.getActionCommand();
        int cardID = -1;
        switch(type_id){
            case 0:
                //前進ボタン
                cardID = getCardID(command);
                if(cardID != -1){
                    try {
                        int cardNum = this.myCardList.get(cardID);
                        this.sendForwardMessage(cardNum);
                    } catch (IOException ex) {
                        Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case 1:
                //後退ボタン
                cardID = getCardID(command);
                if(cardID != -1){
                    try {
                        int cardNum = this.myCardList.get(cardID);
                        this.sendBackwardMessage(cardNum);
                    } catch (IOException ex) {
                        Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case 2:
                //攻撃ボタン
                ArrayList<String> selectedList = (ArrayList<String>)e.getSource();
                int cardNum = -1;
                int cardCount = 0;
                for(String cmd:selectedList){
                    cardID = getCardID(cmd);
                    int num = this.myCardList.get(cardID);
                    if(cardNum == -1){
                        cardNum = num;
                        cardCount = 1;
                    } else if(cardNum == num){
                        cardCount++;
                    }
                }
                try {
                    this.sendAttackMessage(cardNum,cardCount);
                } catch (IOException ex) {
                    Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                break;
        }
    }

    private void selectAction() {
        if(this.myCardList.isEmpty()){
            this.printMessage("myCardList is Empty");
            return;
        }
        //AIがランダムに手札内の数字を選択
        int cardNum = new Random().nextInt(this.myCardList.size());
        int selectedNum = this.myCardList.get(cardNum);
        
        this.printMessage("AI hand = " + this.myCardList.toString());
        this.printMessage("cardNumber = " + selectedNum);
        this.printMessage("cardNum = " + cardNum);

        this.printMessage("player0 position = " + this.p0position);
        this.printMessage("player1 position = " + this.p1position);
        this.printMessage("distance = " + this.distance);

        // 行動の選択肢
        String[] ActionsList = {"Attack","Forward","Backward"};
        List<String> list = new ArrayList<String>(Arrays.asList(ActionsList));

        if(this.p0position - selectedNum < 1 || this.p1position + selectedNum > 23){
            list.remove("Backward");
            this.printMessage("Remove Backward : " + list);
        }
        if(this.distance != selectedNum || this.distance > 5){
            list.remove("Attack");
            this.printMessage("Remove Attack" + list);
        } 
        if(this.distance < selectedNum || this.distance == selectedNum){
            list.remove("Forward");
            this.printMessage("Remove Forward" + list);
        }

        if(list.isEmpty()){
            this.printMessage("List is Empty");
            this.selectAction();
        } else {
            String[] Actions = (String[]) list.toArray(new String[list.size()]);
            int num = (int) (Math.random() * Actions.length);
            this.printMessage("List random number = " + num);
            String selectedAction = Actions[num];
            this.printMessage("action = " + selectedAction);
            this.cardNumber = cardNum;
            this.action = selectedAction;
            // this.setCardNumberAndAction(cardNum, selectedAction);
        }
    }

    // 選択されたカードと同じ数字を持つカードの枚数をランダムで選択するメソッド
    public int selectCardCount(ArrayList<Integer> cardList, int selectedCard) {
        int count = 0;
        for (int card : cardList) {
            if (card == selectedCard) {
                count++;
            }
        }
        // 同じ数字を持つカードが複数ある場合、1枚からその枚数までの間でランダムに選択する
        return new Random().nextInt(count) + 1;
    }

    public void DoPlay(){
        if(myPlayerID == this.currentPlayer){
            this.selectAction();
            String action = this.action;
            int cardID = this.cardNumber;
            //ボタンクリックをコードで実装
            if(action != "" && cardID != -1){
                switch(action){
                    case "Forward":
                        //前進ボタン
                        try {
                            int number = this.myCardList.get(cardID);
                            this.sendForwardMessage(number);
                        } catch (IOException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case "Backward":
                        //後退ボタン
                        try {
                            int number = this.myCardList.get(cardID);
                            this.sendBackwardMessage(number);
                        } catch (IOException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case "Attack":
                        //攻撃ボタン
                        try {
                            int number = this.myCardList.get(cardID);
                            int cardCount = this.selectCardCount(this.myCardList, number);
                            this.sendAttackMessage(number,cardCount);
                        } catch (IOException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(mainFrameAI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break; 
                }
            } else{
                this.printMessage("AI selected error");
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}

class MessageReceiver extends Thread {
    private mainFrameAI parent;
    private BufferedReader serverReader;
    private StringBuilder sbuf;
    
//    public static Pattern jsonStartEnd = Pattern.compile(".*?<json>(.*)</json>.*?");
//    public static Pattern jsonEnd = Pattern.compile("(.*)</json>.*?");
//    public static Pattern jsonStart = Pattern.compile(".*?<json>(.*)");
    public static Pattern jsonStartEnd = Pattern.compile(".*?\\{(.*)\\}.*?");
    public static Pattern jsonEnd = Pattern.compile("(.*)\\}.*?");
    public static Pattern jsonStart = Pattern.compile(".*?\\{(.*)");
    public MessageReceiver(mainFrameAI p){
        this.parent = p;
        this.serverReader = this.parent.getServerReader();
        this.sbuf = new StringBuilder();
    }
    
    @Override
    public void run(){
        try {
            String line;
            while((line = this.serverReader.readLine()) != null){
                Matcher startend = jsonStartEnd.matcher(line);
                Matcher end = jsonEnd.matcher(line);
                Matcher start = jsonStart.matcher(line);
                if(startend.matches()){
                    sbuf = new StringBuilder();
                    sbuf.append("{");
                    sbuf.append(startend.group(1));
                    sbuf.append("}{");
                    this.parent.receiveMessageFromServer(sbuf.toString());
                } else if(end.matches()){
                    sbuf.append(end.group(1));
                    sbuf.append("}{");
                    this.parent.receiveMessageFromServer(sbuf.toString());
                } else if(start.matches()){
                    sbuf = new StringBuilder();
                    sbuf.append("{");
                    sbuf.append(start.group(1));
                } else {
                    sbuf.append(line);
                }
            }
        } catch (IOException ex) {
        }
    }
}

