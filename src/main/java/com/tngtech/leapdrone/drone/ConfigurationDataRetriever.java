package com.tngtech.leapdrone.drone;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.tngtech.leapdrone.drone.config.DroneControllerConfig;
import com.tngtech.leapdrone.drone.data.DroneConfiguration;
import com.tngtech.leapdrone.drone.listeners.DroneConfigurationListener;
import com.tngtech.leapdrone.drone.listeners.ReadyStateChangeListener;
import com.tngtech.leapdrone.helpers.components.AddressComponent;
import com.tngtech.leapdrone.helpers.components.ReadyStateComponent;
import com.tngtech.leapdrone.helpers.components.TcpComponent;
import com.tngtech.leapdrone.helpers.components.ThreadComponent;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ConfigurationDataRetriever implements Runnable
{
  public static final String SEPARATOR = " = ";

  private final Logger logger = Logger.getLogger(ConfigurationDataRetriever.class.getSimpleName());

  private final ThreadComponent threadComponent;

  private final AddressComponent addressComponent;

  private final TcpComponent tcpComponent;

  private final ReadyStateComponent readyStateComponent;

  private final Set<DroneConfigurationListener> droneConfigurationListeners;

  @Inject
  public ConfigurationDataRetriever(ThreadComponent threadComponent, AddressComponent addressComponent, TcpComponent tcpComponent,
                                    ReadyStateComponent readyStateComponent)
  {
    this.threadComponent = threadComponent;
    this.addressComponent = addressComponent;
    this.tcpComponent = tcpComponent;
    this.readyStateComponent = readyStateComponent;

    droneConfigurationListeners = Sets.newHashSet();
  }

  public void start()
  {
    logger.info("Starting config data thread");
    threadComponent.start(this);
  }

  public void stop()
  {
    logger.info("Stopping config data thread");
    threadComponent.stop();
  }

  public void addReadyStateChangeListener(ReadyStateChangeListener readyStateChangeListener)
  {
    readyStateComponent.addReadyStateChangeListener(readyStateChangeListener);
  }

  public void removeReadyStateChangeListener(ReadyStateChangeListener readyStateChangeListener)
  {
    readyStateComponent.addReadyStateChangeListener(readyStateChangeListener);
  }

  public void addDroneConfigurationListener(DroneConfigurationListener droneConfigurationListener)
  {
    if (!droneConfigurationListeners.contains(droneConfigurationListener))
    {
      droneConfigurationListeners.add(droneConfigurationListener);
    }
  }

  public void removeDroneConfigurationListener(DroneConfigurationListener droneConfigurationListener)
  {
    if (droneConfigurationListeners.contains(droneConfigurationListener))
    {
      droneConfigurationListeners.remove(droneConfigurationListener);
    }
  }

  @Override
  public void run()
  {
    connectToConfigDataPort();
    readyStateComponent.emitReadyStateChange(ReadyStateChangeListener.ReadyState.READY);

    while (!threadComponent.isStopped())
    {
      try
      {
        processData(readLines());
      } catch (RuntimeException e)
      {
        logger.error("Error processing the config control data", e);
      }
    }

    disconnectFromConfigDataPort();
  }

  private void connectToConfigDataPort()
  {
    logger.info(String.format("Connecting to config data port %d", DroneControllerConfig.CONFIG_DATA_PORT));
    tcpComponent.connect(addressComponent.getInetAddress(DroneControllerConfig.DRONE_IP_ADDRESS), DroneControllerConfig.CONFIG_DATA_PORT, 1000);
  }

  public Collection<String> readLines()
  {
    try
    {
      return doReadLines();
    } catch (IOException | ClassNotFoundException e)
    {
      throw new IllegalStateException("Error receiving current lines", e);
    }
  }

  private Collection<String> doReadLines() throws IOException, ClassNotFoundException
  {
    Collection<String> receivedLines = Lists.newArrayList();

    try
    {
      String line = tcpComponent.getReader().readLine();
      while (line != null)
      {
        receivedLines.add(line);
        line = tcpComponent.getReader().readLine();
      }
    } catch (SocketTimeoutException e)
    {
      // EOF is reached (this is a dirty workaround, but there is no indicator telling us when to stop)
    }

    return receivedLines;
  }

  private void processData(Collection<String> lines)
  {
    if (lines.size() == 0)
    {
      return;
    }

    logger.info("Drone configuration data received");
    DroneConfiguration droneConfiguration = getDroneConfiguration(lines);

    for (DroneConfigurationListener listener : droneConfigurationListeners)
    {
      listener.onDroneConfiguration(droneConfiguration);
    }
  }

  private DroneConfiguration getDroneConfiguration(Collection<String> lines)
  {
    Map<String, String> configMap = Maps.newHashMap();

    for (String line : lines)
    {
      String[] configOption = line.split(SEPARATOR);
      if (configOption.length != 2)
      {
        continue;
      }

      configMap.put(configOption[0], configOption[1]);
    }

    return new DroneConfiguration(configMap);
  }

  private void disconnectFromConfigDataPort()
  {
    logger.info(String.format("Connecting to config data port %d", DroneControllerConfig.CONFIG_DATA_PORT));
    tcpComponent.disconnect();
  }
}