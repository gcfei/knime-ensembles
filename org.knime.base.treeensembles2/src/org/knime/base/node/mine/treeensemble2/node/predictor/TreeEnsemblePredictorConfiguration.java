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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.Optional;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * The configuration for predictions made with a tree ensemble based model e.g. random forests or gradient boosted
 * trees.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TreeEnsemblePredictorConfiguration extends AbstractPredictorConfiguration {

    private static final String CFG_APPEND_PREDICTION_CONFIDENCE = "appendPredictionConfidence";

    private static final String CFG_APPEND_CLASS_CONFIDENCES = "appendClassConfidences";

    private static final String CFG_SUFFIX_FOR_CLASS_PROBABILITIES = "suffixForClassProbabilities";

    private static final String CFG_APPEND_MODEL_COUNT = "appendModelCount";

    private static final String CFG_USE_SOFT_VOTING = "useSoftVoting";

    /**
     * @param targetColName name of the target column
     * @return the default name for the Prediction column
     */
    public static final String getPredictColumnName(final String targetColName) {
        return "Prediction (" + targetColName + ")";
    }

    /**
     * Only use this method if the target column name is not known.
     *
     * @return default name for the prediction column
     */
    public static final String getDefPredictColumnName() {
        return getPredictColumnName("");
    }

    @SuppressWarnings("unused")
    private final boolean m_isRegression;

    private boolean m_appendPredictionConfidence = true;

    private boolean m_appendClassConfidences = false;

    private String m_suffixForClassProbabilities = "";

    private boolean m_appendModelCount = false;

    private boolean m_useSoftVoting = false;

    /**
     * @param isRegression
     * @param targetColName
     *
     */
    public TreeEnsemblePredictorConfiguration(final boolean isRegression, final String targetColName) {
        super(targetColName);
        m_isRegression = isRegression;
    }

    /**
     * @return the appendPredictionConfidence
     */
    public boolean isAppendPredictionConfidence() {
        return m_appendPredictionConfidence;
    }

    /**
     * @param value the appendPredictionConfidence to set
     */
    public void setAppendPredictionConfidence(final boolean value) {
        m_appendPredictionConfidence = value;
    }

    /**
     * @return the appendConfidenceValues
     */
    public boolean isAppendClassConfidences() {
        return m_appendClassConfidences;
    }

    /**
     * @param value the appendClassConfidences to set
     */
    public void setAppendClassConfidences(final boolean value) {
        m_appendClassConfidences = value;
    }

    /**
     * @return the suffix for class probability columns
     */
    public String getSuffixForClassProbabilities() {
        return m_suffixForClassProbabilities;
    }

    /**
     * @param suffixForClassConfidences the suffix for class probabilities to set
     */
    public void setSuffixForClassConfidences(final String suffixForClassConfidences) {
        m_suffixForClassProbabilities = suffixForClassConfidences;
    }

    /**
     * @return the appendModelCount
     */
    public boolean isAppendModelCount() {
        return m_appendModelCount;
    }

    /**
     * @param appendModelCount the appendModelCount to set
     */
    public void setAppendModelCount(final boolean appendModelCount) {
        m_appendModelCount = appendModelCount;
    }

    /**
     * @return boolean to indicate if soft voting is enabled
     */
    public boolean isUseSoftVoting() {
        return m_useSoftVoting;
    }

    /**
     * In case the configuration is set to {@link #isUseSoftVoting() soft voting} and the model contains no class
     * distribution, return a warning message. Otherwise return an empty optional.
     *
     * @param model The non-null model to check.
     * @return An optional warning message.
     */
    public Optional<String> checkSoftVotingSettingForModel(final TreeEnsembleModelPortObject model) {
        if (isUseSoftVoting() && !model.getEnsembleModel().containsClassDistribution()) {
            return Optional.of("The tree ensemble does not contain the target distributions"
                + "; soft voting will have the same results as hard voting.");
        }
        return Optional.empty();
    }

    /**
     * @param useSoftVoting boolean that indicates whether soft voting should be enabled
     */
    public void setUseSoftVoting(final boolean useSoftVoting) {
        m_useSoftVoting = useSoftVoting;
    }

    /**
     * Saves the configuration settings
     *
     * @param settings
     */
    @Override
    public void internalSave(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_APPEND_PREDICTION_CONFIDENCE, m_appendPredictionConfidence);
        settings.addBoolean(CFG_APPEND_CLASS_CONFIDENCES, m_appendClassConfidences);
        settings.addBoolean(CFG_APPEND_MODEL_COUNT, m_appendModelCount);
        settings.addString(CFG_SUFFIX_FOR_CLASS_PROBABILITIES, m_suffixForClassProbabilities);
        settings.addBoolean(CFG_USE_SOFT_VOTING, m_useSoftVoting);
    }

    /**
     * Use this method to load the settings in the NodeDialog
     *
     * @param settings
     * @throws NotConfigurableException
     */
    @Override
    public void internalLoadInDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        m_appendPredictionConfidence = settings.getBoolean(CFG_APPEND_PREDICTION_CONFIDENCE, true);
        m_appendClassConfidences = settings.getBoolean(CFG_APPEND_CLASS_CONFIDENCES, false);
        m_appendModelCount = settings.getBoolean(CFG_APPEND_MODEL_COUNT, false);

        m_suffixForClassProbabilities = settings.getString(CFG_SUFFIX_FOR_CLASS_PROBABILITIES, "");
        m_useSoftVoting = settings.getBoolean(CFG_USE_SOFT_VOTING, false);
    }

    /**
     * Use this method to load the settings in the NodeModel
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    @Override
    public void internalLoadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_appendPredictionConfidence = settings.getBoolean(CFG_APPEND_PREDICTION_CONFIDENCE);
        m_appendClassConfidences = settings.getBoolean(CFG_APPEND_CLASS_CONFIDENCES);
        m_appendModelCount = settings.getBoolean(CFG_APPEND_MODEL_COUNT);

        m_suffixForClassProbabilities = settings.getString(CFG_SUFFIX_FOR_CLASS_PROBABILITIES);
        // added in 3.3
        m_useSoftVoting = settings.getBoolean(CFG_USE_SOFT_VOTING, false);
    }

    /**
     * Intended for the use in the configure method of the NodeModel to autoconfigure the node.
     *
     * @param isRegression
     * @param targetColName
     * @return a default configuration
     */
    public static TreeEnsemblePredictorConfiguration createDefault(final boolean isRegression,
        final String targetColName) {
        return new TreeEnsemblePredictorConfiguration(isRegression, targetColName);
    }

}
