package com.rpa.manage.service.impl.robot;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.domain.dto.robot.RobotPageQuery;
import com.rpa.manage.domain.dto.robot.RobotUpsertRequest;
import com.rpa.manage.domain.entity.RpaRobot;
import com.rpa.manage.domain.repository.RpaRobotRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.service.robot.RobotService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 机器人管理服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RobotServiceImpl implements RobotService {

    private final RpaRobotRepository rpaRobotRepository;
    private final RpaTaskRepository rpaTaskRepository;

    /**
     * 获取机器人概览统计。
     * 返回总数以及离线、在线、工作中的数量分布。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", rpaRobotRepository.count());
        result.put("onlineCount", rpaRobotRepository.countByStatus(1));
        result.put("workingCount", rpaRobotRepository.countByStatus(2));
        result.put("offlineCount", rpaRobotRepository.countByStatus(0));
        return result;
    }

    /**
     * 机器人分页查询。
     * 支持按机器人名称、编码和状态过滤。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> page(RobotPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );

        Specification<RpaRobot> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getRobotName())) {
                predicates.add(cb.like(root.get("robotName"), "%" + query.getRobotName().trim() + "%"));
            }
            if (StringUtils.hasText(query.getRobotCode())) {
                predicates.add(cb.like(root.get("robotCode"), "%" + query.getRobotCode().trim() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<RpaRobot> page = rpaRobotRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toRobotView)
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 新增机器人。
     */
    @Override
    @Transactional
    public Map<String, Object> create(RobotUpsertRequest request) {
        String robotCode = request.getRobotCode().trim();
        if (rpaRobotRepository.existsByRobotCode(robotCode)) {
            throw new BusinessException("机器人编码已存在");
        }

        RpaRobot robot = new RpaRobot();
        applyRobot(robot, request);
        rpaRobotRepository.save(robot);
        log.info("创建机器人完成，robotId={}, robotCode={}, robotName={}", robot.getId(), robot.getRobotCode(), robot.getRobotName());
        return toRobotView(robot);
    }

    /**
     * 查询机器人详情。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        return toRobotView(getRobot(id));
    }

    /**
     * 修改机器人基础信息。
     */
    @Override
    @Transactional
    public Map<String, Object> update(Long id, RobotUpsertRequest request) {
        RpaRobot robot = getRobot(id);
        String robotCode = request.getRobotCode().trim();
        if (rpaRobotRepository.existsByRobotCodeAndIdNot(robotCode, id)) {
            throw new BusinessException("机器人编码已存在");
        }

        applyRobot(robot, request);
        rpaRobotRepository.save(robot);
        log.info("修改机器人完成，robotId={}, robotCode={}, robotName={}", robot.getId(), robot.getRobotCode(), robot.getRobotName());
        return toRobotView(robot);
    }

    /**
     * 删除机器人。
     * 工作中的机器人或已被任务绑定的机器人不允许删除。
     */
    @Override
    @Transactional
    public Map<String, Object> delete(Long id) {
        RpaRobot robot = getRobot(id);
        if (robot.getStatus() != null && robot.getStatus() == 2) {
            throw new BusinessException("机器人正在工作中，不能删除");
        }
        if (rpaTaskRepository.countByRobotId(id) > 0) {
            throw new BusinessException("该机器人已被任务绑定，不能删除");
        }

        rpaRobotRepository.delete(robot);
        log.info("删除机器人完成，robotId={}, robotCode={}, robotName={}", id, robot.getRobotCode(), robot.getRobotName());
        return Map.of("id", id);
    }

    /**
     * 按主键加载机器人，不存在时抛出业务异常。
     */
    private RpaRobot getRobot(Long id) {
        return rpaRobotRepository.findById(id)
                .orElseThrow(() -> new BusinessException("机器人不存在"));
    }

    /**
     * 将请求中的机器人字段写回实体，并校验允许编辑的状态值。
     */
    private void applyRobot(RpaRobot robot, RobotUpsertRequest request) {
        validateEditableStatus(request.getStatus());
        robot.setRobotCode(request.getRobotCode().trim());
        robot.setRobotName(request.getRobotName().trim());
        robot.setRobotType(request.getRobotType().trim());
        robot.setDescription(normalizeNullable(request.getDescription()));
        robot.setStatus(request.getStatus());
    }

    /**
     * 组装机器人列表和详情通用视图。
     */
    private Map<String, Object> toRobotView(RpaRobot robot) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", robot.getId());
        result.put("robotCode", robot.getRobotCode());
        result.put("robotName", robot.getRobotName());
        result.put("robotType", robot.getRobotType());
        result.put("description", robot.getDescription());
        result.put("status", robot.getStatus());
        result.put("currentTaskId", robot.getCurrentTaskId());
        result.put("lastHeartbeatTime", robot.getLastHeartbeatTime());
        result.put("createTime", robot.getCreateTime());
        result.put("updateTime", robot.getUpdateTime());
        return result;
    }

    /**
     * 将空白字符串标准化为 null，避免保存无意义空串。
     */
    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 机器人新增和编辑时，只允许手工维护“离线/在线”两种基础状态。
     * “工作中”属于运行态，应由后续任务调度或执行流程自动切换，不能由页面直接传入。
     */
    private void validateEditableStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("机器人状态仅允许设置为0-离线或1-在线");
        }
    }

    /**
     * 兜底分页页码，确保最小值为 1。
     */
    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    /**
     * 兜底分页大小，并限制单页最大查询量。
     */
    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }
}
