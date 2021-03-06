/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   31.03.2011 (meinl): created
 */
package org.knime.ensembles.boosting;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.model.PortObjectValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This is the model for the start node of a boosting prediction node. It takes
 * the input table (with models and weights) and output a single model in each
 * iteration. The model's weight can be queried by the corresponding loop end
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BoostingPredictorLoopStartNodeModel extends NodeModel implements
        LoopStartNodeTerminator {
    private final BoostingPredictorStartSettings m_settings =
            new BoostingPredictorStartSettings();

    private CloseableRowIterator m_iterator;

    private double m_currentModelWeight;

    /**
     * Creates a new node model.
     */
    public BoostingPredictorLoopStartNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{PortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];

        if (m_settings.modelColumn() == null) {
            for (DataColumnSpec cs : inSpec) {
                if (cs.getType().isCompatible(PortObjectValue.class)) {
                    m_settings.modelColumn(cs.getName());
                    setWarningMessage("Auto-selected column '" + cs.getName()
                            + "' as model column");
                    break;
                }
            }
            if (m_settings.modelColumn() == null) {
                throw new InvalidSettingsException(
                        "No double column for model found in input table");
            }
        }
        DataColumnSpec mSpec = inSpec.getColumnSpec(m_settings.modelColumn());
        if (mSpec == null) {
            throw new InvalidSettingsException("Model column '"
                    + m_settings.modelColumn()
                    + "' does not exist in input table");
        }
        if (!mSpec.getType().isCompatible(PortObjectValue.class)) {
            throw new InvalidSettingsException("Model column '"
                    + m_settings.modelColumn() + "' is not a model column");
        }

        if (m_settings.weightColumn() == null) {
            for (DataColumnSpec cs : inSpec) {
                if (cs.getType().isCompatible(DoubleValue.class)) {
                    m_settings.weightColumn(cs.getName());
                    setWarningMessage("Auto-selected column '" + cs.getName()
                            + "' as weight column");
                    break;
                }
            }
            if (m_settings.weightColumn() == null) {
                throw new InvalidSettingsException(
                        "No double column for model weights "
                        + "found in first input table");
            }
        }
        DataColumnSpec wSpec = inSpec.getColumnSpec(m_settings.weightColumn());
        if (wSpec == null) {
            throw new InvalidSettingsException("Weight column '"
                    + m_settings.weightColumn()
                    + "' does not exist in first input table");
        }
        if (!wSpec.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("Weight column '"
                    + m_settings.weightColumn() + "' is not a double column");
        }

        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = (BufferedDataTable)inObjects[0];
        if (getLoopEndNode() == null) {
            // first iteration
            m_iterator = table.iterator();
        }

        DataRow row = m_iterator.next();
        int weightIndex =
                table.getSpec().findColumnIndex(m_settings.weightColumn());
        DataCell weightCell = row.getCell(weightIndex);
        if (weightCell.isMissing()) {
            throw new Exception("Missing values are not supported");
        }
        m_currentModelWeight = ((DoubleValue)weightCell).getDoubleValue();

        int modelIndex =
                table.getSpec().findColumnIndex(m_settings.modelColumn());
        DataCell modelCell = row.getCell(modelIndex);
        if (modelCell.isMissing()) {
            throw new Exception("Missing values are not supported");
        }
        return new PortObject[]{((PortObjectValue)modelCell).getPortObject()};
    }

    /** @return the weight of the current model. */
    double getCurrentModelWeight() {
        return m_currentModelWeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        BoostingPredictorStartSettings s = new BoostingPredictorStartSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_iterator != null) {
            m_iterator.close();
            m_iterator = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return (m_iterator != null) && !m_iterator.hasNext();
    }
}
