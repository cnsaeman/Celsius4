package celsius.gui;

import celsius.Resources;
import java.awt.GridLayout;
import javax.swing.*;

public class RangeEditor extends JPanel {

    private final Resources RSC;
    private JTextField jTFValue;
    private ButtonGroup BtnGrp;
    private JRadioButton Btn1,Btn2;
    
    public RangeEditor(String s,Resources rsc) {
        super();
        RSC=rsc;
        setLayout(new GridLayout(0,1));
        JPanel pnl=new JPanel();
        this.setLayout(new GridLayout(0,1,0,0));
        JLabel jl=new JLabel(s);
        jl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jl.setFont(new java.awt.Font("Arial", 0, RSC.guiTools.guiScale(11)));
        pnl.add(jl);
        BtnGrp=new ButtonGroup();
        Btn1=new JRadioButton("<");
        BtnGrp.add(Btn1);
        Btn1.setSelected(true);
        Btn1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        Btn2=new JRadioButton(">");
        Btn2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        BtnGrp.add(Btn2);
        pnl.add(Btn1);
        pnl.add(Btn2);
        this.add(pnl);
        jTFValue=new JTextField();
        this.add(jTFValue);
    }

    public boolean isBiggerSelected() {
        return(Btn2.isSelected());
    }

    public String getValue() {
        return(jTFValue.getText());
    }

}

