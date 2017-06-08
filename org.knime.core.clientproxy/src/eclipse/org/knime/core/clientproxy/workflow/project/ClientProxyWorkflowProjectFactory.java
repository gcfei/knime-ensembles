/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Nov 29, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.api.node.workflow.project.ProjectTreeNode;
import org.knime.core.api.node.workflow.project.WorkflowProject;
import org.knime.core.api.node.workflow.project.WorkflowProjectFactory;
import org.knime.core.internal.KNIMEPath;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.util.LockFailedException;

/**
 * Mainly for testing and prototyping purposes. It provides all workflows of the workspace as a project tree such that
 * the analytics platform itself can serve as a server (see also DefaultWorkflowService).
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyWorkflowProjectFactory implements WorkflowProjectFactory {

    /**
     *
     */
    public ClientProxyWorkflowProjectFactory() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProjectTreeNode getProjectTree() {
        return new MyProjectTreeNode(KNIMEPath.getWorkspaceDirPath().toPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasProjectTreeChanged() {
        return false;
    }

    private class MyProjectTreeNode implements ProjectTreeNode {

        private Path m_path;

        private List<ProjectTreeNode> m_children = null;

        private List<WorkflowProject> m_wfChildren = null;

        /**
         *
         */
        public MyProjectTreeNode(final Path path) {
            m_path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_path.getFileName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getID() {
            return m_path.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ProjectTreeNode> getChildren() {
            resolveChildren();
            return m_children;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<WorkflowProject> getChildrenProjects() {
            resolveChildren();
            return m_wfChildren;
        }

        private void resolveChildren() {
            if (m_children == null) {
                //not resolved, yet
                m_children = new ArrayList<ProjectTreeNode>();
                m_wfChildren = new ArrayList<WorkflowProject>();
                try {
                    Files.list(m_path).forEach((p) -> {
                        if (Files.isDirectory(p)) {
                            if (Files.exists(p.resolve("workflow.knime"))) {
                                m_wfChildren.add(new MyWorkflowProject(p));
                            } else {
                                m_children.add(new MyProjectTreeNode(p));
                            }
                        }
                    });
                } catch (IOException ex) {
                    // TODO better exception handling
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private class MyWorkflowProject implements WorkflowProject {

        private Path m_path;

        /**
         *
         */
        public MyWorkflowProject(final Path path) {
            m_path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_path.getFileName().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getID() {
            return m_path.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WorkflowProjectType getType() {
            return WorkflowProjectType.LOCAL;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IWorkflowManager openProject() {
            try {
                return org.knime.core.node.workflow.WorkflowManager
                    .loadProject(m_path.toFile(), new ExecutionMonitor(), new WorkflowLoadHelper()).getWorkflowManager();
            } catch (IOException | InvalidSettingsException | CanceledExecutionException
                    | UnsupportedWorkflowVersionException | LockFailedException ex) {
                // TODO better exception handling here
                throw new RuntimeException(ex);
            }
        }

    }
}
