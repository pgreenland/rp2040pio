/*
 * @(#)GPIOArrayPanel.java 1.00 21/04/10
 *
 * Copyright (C) 2021 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For updates and more info or contacting the author, visit:
 * <https://github.com/soundpaint/rp2040pio>
 *
 * Author's web site: www.juergen-reuter.de
 */
package org.soundpaint.rp2040pio.observer.gpio;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.soundpaint.rp2040pio.Bit;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.Direction;
import org.soundpaint.rp2040pio.GPIOIOBank0Registers;
import org.soundpaint.rp2040pio.sdk.SDK;

public class GPIOPanel extends JPanel
{
  private static final long serialVersionUID = 2863884535718464586L;

  private final PrintStream console;
  private final SDK sdk;
  private final int refresh;
  private final int gpioNum;
  private final ImageIcon ledGreenOff;
  private final ImageIcon ledGreenOn;
  private final ImageIcon ledRedOff;
  private final ImageIcon ledRedOn;
  private final JLabel lbStatus;

  private GPIOPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIOPanel(final PrintStream console, final SDK sdk,
                   final int refresh, final int gpioNum,
                   final ImageIcon ledGreenOff, final ImageIcon ledGreenOn,
                   final ImageIcon ledRedOff, final ImageIcon ledRedOn)
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    Objects.requireNonNull(ledGreenOff);
    Objects.requireNonNull(ledGreenOn);
    Objects.requireNonNull(ledRedOff);
    Objects.requireNonNull(ledRedOn);
    this.console = console;
    this.sdk = sdk;
    this.refresh = refresh;
    this.gpioNum = gpioNum;
    this.ledGreenOff = ledGreenOff;
    this.ledGreenOn = ledGreenOn;
    this.ledRedOff = ledRedOff;
    this.ledRedOn = ledRedOn;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createVerticalStrut(5));
    final Box gpioNumBox = new Box(BoxLayout.X_AXIS);
    gpioNumBox.add(Box.createHorizontalGlue());
    gpioNumBox.add(new JLabel(String.format("%d", gpioNum)));
    gpioNumBox.add(Box.createHorizontalGlue());
    add(gpioNumBox);
    add(Box.createVerticalStrut(5));
    final Box ledBox = new Box(BoxLayout.X_AXIS);
    ledBox.add(Box.createHorizontalGlue());
    ledBox.add(lbStatus = new JLabel(ledGreenOff));
    ledBox.add(Box.createHorizontalGlue());
    add(ledBox);
    add(Box.createVerticalGlue());
    setMaximumSize(getPreferredSize());
    new Thread(() -> updateStatus()).start();
  }

  private void updateStatus(final int statusValue)
  {
    final int gpioOeFromPeri =
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OEFROMPERI_LSB;
    final Direction oeValue = Direction.fromValue(gpioOeFromPeri);
    final int gpioOutFromPeri =
      (statusValue & Constants.IO_BANK0_GPIO0_STATUS_OUTFROMPERI_BITS) >>>
      Constants.IO_BANK0_GPIO0_STATUS_OUTFROMPERI_LSB;
    final Bit outValue = Bit.fromValue(gpioOutFromPeri);
    final ImageIcon icon =
      oeValue == Direction.IN ?
      (outValue == Bit.HIGH ? ledGreenOn : ledGreenOff) :
      (outValue == Bit.HIGH ? ledRedOn : ledRedOff);
    if (icon != lbStatus.getIcon()) {
      lbStatus.setIcon(icon);
      SwingUtilities.invokeLater(() -> repaint());
    }
  }

  public void updateStatus()
  {
    while (true) {
      final int statusAddress =
        GPIOIOBank0Registers.
        getGPIOAddress(gpioNum, GPIOIOBank0Registers.Regs.GPIO0_STATUS);
      try {
        while (true) {
          updateStatus(sdk.readAddress(statusAddress));
          try {
            Thread.sleep(refresh);
          } catch (final InterruptedException e) {
            // ignore
          }
        }
      } catch (final IOException e) {
        console.println(e.getMessage());
      }
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */