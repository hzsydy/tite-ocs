/* 
 *
 * Copyright (c) 2000-2012 by Rodney Kinney, Joel Uckelman, George Hayward
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package net.cantab.hayward.george.MAP;

import VASSAL.build.GameModule;
import VASSAL.build.module.map.BoardPicker;
import VASSAL.build.module.map.DrawPile;
import VASSAL.build.module.map.StackMetrics;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.build.module.map.boardPicker.board.MapGrid;
import VASSAL.build.module.map.boardPicker.board.Region;
import VASSAL.build.module.map.boardPicker.board.RegionGrid;
import VASSAL.build.module.map.boardPicker.board.ZonedGrid;
import VASSAL.build.module.map.boardPicker.board.mapgrid.Zone;
import VASSAL.command.Command;
import VASSAL.i18n.Resources;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;

/**
 * This class is the replacement for VASSAL.build.module.MAP which preserves
 * the back end piece storage but has none of the front end graphics or window
 * manipulation. It is not a complete re-implementation as it only does enough
 * to support the modules I wanted to convert.
 * <P>
 * The first map defined is the master map which is displayed initially and all
 * other maps are displayed via their bookmarks within the master map. The master
 * map functionality is implemented within the MasterMap class.
 * <P>
 * As part of saving and loading games this object saves and restores it's list
 * of bookmarks.
 * @author George Hayward
 */
public class NewMap extends OldMap {

    /**
     * This is the size of the map.
     */
    protected Dimension sizeOfMap;
    
    
    /*
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     */
    
    

    @Override
    public String getAttributeValueString(String key) {
        return null;
    }

    // TODO: Evaluate setBoardPicker requirement
    /**
     * Every map must include a {@link BoardPicker} as one of its build
     * components. This method is called by a BoardPicker instance as it is
     * added to the map.
     *
     * @param picker
     */
    @Override
    public void setBoardPicker(BoardPicker picker) {
        if (this.picker != null) {
            GameModule.getGameModule().removeCommandEncoder(picker);
            GameModule.getGameModule().getGameState().addGameComponent(picker);
        }
        this.picker = picker;
        if (picker != null) {
            picker.setAllowMultiple(allowMultiple);
            GameModule.getGameModule().addCommandEncoder(picker);
            GameModule.getGameModule().getGameState().addGameComponent(picker);
        }
    }

    // TODO: Evaluate setStackMetrics() requirement
    /**
     * Every map must include a {@link StackMetrics} as one of its build
     * components, which governs the stacking behaviour of GamePieces on the
     * map. This method is called by a StackMetrics instance as it is added to
     * the map.
     */
    public void setStackMetrics(StackMetrics sm) {
        metrics = sm;
    }

    // TODO: Evaluate setPieceMover() requirement
    //TODO: Evaluate setBoards
    /**
     * Set the boards for this map. Each map may contain more than one
     * {@link Board}.
     */
    public synchronized void setBoards(Collection<Board> c) {
        boards.clear();
        for (Board b : c) {
            b.setMap(this);
            boards.add(b);
        }
        setBoardBoundaries();
    }

    public Command getRestoreCommand() {
        // TODO: generate proper restore command
        return null;
    }

    /**
     * @return the {@link Board} on this map containing the argument point
     */
    public Board findBoard(Point p) {
        for (Board b : boards) {
            if (b.bounds().contains(p)) {
                return b;
            }
        }
        return null;
    }

    /**
     *
     * @return the {@link Zone} on this map containing the argument point
     */
    public Zone findZone(Point p) {
        Board b = findBoard(p);
        if (b != null) {
            MapGrid grid = b.getGrid();
            if (grid != null && grid instanceof ZonedGrid) {
                Rectangle r = b.bounds();
                p.translate(-r.x, -r.y);  // Translate to Board co-ords
                return ((ZonedGrid) grid).findZone(p);
            }
        }
        return null;
    }

    /**
     * Search on all boards for a Zone with the given name
     *
     * @param Zone name
     * @return Located zone
     */
    public Zone findZone(String name) {
        for (Board b : boards) {
            for (ZonedGrid zg : b.getAllDescendantComponentsOf(ZonedGrid.class)) {
                Zone z = zg.findZone(name);
                if (z != null) {
                    return z;
                }
            }
        }
        return null;
    }

    /**
     * Search on all boards for a Region with the given name
     *
     * @param Region name
     * @return Located region
     */
    public Region findRegion(String name) {
        for (Board b : boards) {
            for (RegionGrid rg : b.getAllDescendantComponentsOf(RegionGrid.class)) {
                Region r = rg.findRegion(name);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Return the board with the given name
     *
     * @param name
     * @return null if no such board found
     */
    public Board getBoardByName(String name) {
        if (name != null) {
            for (Board b : boards) {
                if (name.equals(b.getName())) {
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * @return true if the given point may not be a legal location. I.e., if
     * this grid will attempt to snap it to the nearest grid location
     */
    public boolean isLocationRestricted(Point p) {
        Board b = findBoard(p);
        if (b != null) {
            Rectangle r = b.bounds();
            Point snap = new Point(p);
            snap.translate(-r.x, -r.y);
            return b.isLocationRestricted(snap);
        } else {
            return false;
        }
    }

    /**
     * @return the nearest allowable point according to the {@link VASSAL.build.module.map.boardPicker.board.MapGrid}
     * on the {@link Board} at this point
     *
     * @see Board#snapTo
     * @see VASSAL.build.module.map.boardPicker.board.MapGrid#snapTo
     */
    public Point snapTo(Point p) {
        Point snap = new Point(p);

        final Board b = findBoard(p);
        if (b == null) {
            return snap;
        }

        final Rectangle r = b.bounds();
        snap.translate(-r.x, -r.y);
        snap = b.snapTo(snap);
        snap.translate(r.x, r.y);
        // RFE 882378
        // If we have snapped to a point 1 pixel off the edge of the map, move
        // back
        // onto the map.
        if (findBoard(snap) == null) {
            snap.translate(-r.x, -r.y);
            if (snap.x == r.width) {
                snap.x = r.width - 1;
            } else if (snap.x == -1) {
                snap.x = 0;
            }
            if (snap.y == r.height) {
                snap.y = r.height - 1;
            } else if (snap.y == -1) {
                snap.y = 0;
            }
            snap.translate(r.x, r.y);
        }
        return snap;
    }

    /**
     * @return a String name for the given location on the map
     *
     * @see Board#locationName
     */
    public String locationName(Point p) {
        String loc = getDeckNameAt(p);
        if (loc == null) {
            Board b = findBoard(p);
            if (b != null) {
                loc = b.locationName(new Point(p.x - b.bounds().x, p.y - b.bounds().y));
            }
        }
        if (loc == null) {
            loc = Resources.getString("Map.offboard"); //$NON-NLS-1$
        }
        return loc;
    }

    public String localizedLocationName(Point p) {
        String loc = getLocalizedDeckNameAt(p);
        if (loc == null) {
            Board b = findBoard(p);
            if (b != null) {
                loc = b.localizedLocationName(new Point(p.x - b.bounds().x, p.y - b.bounds().y));
            }
        }
        if (loc == null) {
            loc = Resources.getString("Map.offboard"); //$NON-NLS-1$
        }
        return loc;
    }

    /**
     * Return the name of the deck whose bounding box contains p
     */
    public String getDeckNameContaining(Point p) {
        String deck = null;
        if (p != null) {
            for (DrawPile d : getComponentsOf(DrawPile.class)) {
                Rectangle box = d.boundingBox();
                if (box != null && box.contains(p)) {
                    deck = d.getConfigureName();
                    break;
                }
            }
        }
        return deck;
    }

    /**
     * Return the name of the deck whose position is p
     *
     * @param p
     * @return
     */
    public String getDeckNameAt(Point p) {
        String deck = null;
        if (p != null) {
            for (DrawPile d : getComponentsOf(DrawPile.class)) {
                if (d.getPosition().equals(p)) {
                    deck = d.getConfigureName();
                    break;
                }
            }
        }
        return deck;
    }

    public String getLocalizedDeckNameAt(Point p) {
        String deck = null;
        if (p != null) {
            for (DrawPile d : getComponentsOf(DrawPile.class)) {
                if (d.getPosition().equals(p)) {
                    deck = d.getLocalizedConfigureName();
                    break;
                }
            }
        }
        return deck;
    }
}
